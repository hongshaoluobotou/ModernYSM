package com.elfmcys.yesstevemodel.model;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.S2CModelUploadResultPacket;
import com.elfmcys.yesstevemodel.network.message.S2CModelUploadStartPacket;
import com.elfmcys.yesstevemodel.resource.YSMBinaryDeserializer;
import com.elfmcys.yesstevemodel.resource.YSMFolderDeserializer;
import com.elfmcys.yesstevemodel.resource.pojo.RawYsmModel;
import com.elfmcys.yesstevemodel.util.DigestUtil;
import com.elfmcys.yesstevemodel.util.YSMThreadPool;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import rip.ysm.legacy.YesModelUtils;
import rip.ysm.security.YsmCrypt;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 服务端模型上传会话管理（C2SModelUploadStart/Chunk/Finish 的服务端半份，此前为空实现）。
 * <p>
 * 协议约定（与 {@link com.elfmcys.yesstevemodel.client.upload.ModelUploadSession} 对齐）：
 * <ul>
 *   <li>Start：服务端校验后回 {@link S2CModelUploadStartPacket}（status=0 + uploadId + chunkSize +
 *       maxTotalBytes + chunksPerTick，或 status 1-6 错误码，映射见客户端 getRequestErrorText）；</li>
 *   <li>Chunk：任意顺序按 offset 落入预分配缓冲，以片段去重计数；</li>
 *   <li>Finish：全片段齐 + SHA-256 匹配后，先干跑解析（与 {@code ServerModelManager#processYsmFile}
 *       同链），再以 {@code CREATE_NEW} 原子创建写入 {@link ServerModelManager#CUSTOM}（独立
 *       {@code <id>.ysm} 文件，custom 目录即 {@code scanSource} 的受支持 standalone 文件来源），
 *       随后复用 {@link ServerModelManager#loadModels} 重载并把最终模型 ID 经
 *       {@link S2CModelUploadResultPacket} 回告客户端。</li>
 * </ul>
 * <p>
 * <b>文件名策略（2026.10.18 定稿）</b>：不做任何字符白名单/静态拦截——收到的 ID 原样拼
 * {@code <id>.ysm} 直接创建；文件系统支持就成立，不支持（Windows 保留名/非法字符/成分超长等）
 * 由 {@code CREATE_NEW} 写入路径抛 {@link InvalidPathException}/{@link IOException}，按异常类型
 * 映射协议错误码并回传 FS 原始原因。唯一保留的静态边界是<b>沙箱校验</b>：合并出的目标路径必须
 * 仍在 custom 目录内（防 {@code ../}、Windows 盘符相对路径等逃逸 custom 写入任意位置）。
 * 协议字节与客户端发送逻辑不变，连接官方 OpenYSM 服务器时本类不参与，兼容性保持。
 */
public final class ServerModelUploadManager {

    /** 与 ModelUploadSession 默认 chunkSize 一致；包开销后仍低于原版自定义 payload 上限。 */
    public static final int CHUNK_SIZE = 32_000;

    private static final long SESSION_TIMEOUT_MS = 30_000;
    private static final int MAX_CONCURRENT_SESSIONS = 64;
    private static final Pattern SHA256_PATTERN = Pattern.compile("[0-9a-fA-F]{64}");

    private static final AtomicLong NEXT_UPLOAD_ID = new AtomicLong();
    private static final ConcurrentHashMap<Long, UploadSession> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SESSION_BY_PLAYER = new ConcurrentHashMap<>();

    private ServerModelUploadManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> ServerModelUploadManager.tick());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ServerModelUploadManager.removePlayer(handler.getPlayer().getUUID()));
    }

    // ---------- C2S 处理入口 ----------

    public static void onStart(ServerPlayer player, String modelId, int totalBytes, String sha256Hex) {
        UUID playerId = player.getUUID();
        if (!ServerConfig.ALLOW_UPLOAD.get()) {
            replyStart(player, 6, "");
            return;
        }
        if (SESSION_BY_PLAYER.containsKey(playerId)) {
            replyStart(player, 4, "");
            return;
        }
        tick(); // 惰性清理过期会话
        int maxTotalBytes = ServerConfig.MAX_UPLOAD_BYTES.get();
        if (sha256Hex == null || !SHA256_PATTERN.matcher(sha256Hex).matches()) {
            replyStart(player, 5, "非法哈希值");
            YesSteveModel.LOGGER.warn("[YSM] Upload rejected: player={} id='{}' reason=bad-sha", player.getGameProfile().name(), modelId);
            return;
        }
        if (totalBytes <= 0 || totalBytes > maxTotalBytes) {
            replyStart(player, 2, "超过服务器上限（" + maxTotalBytes + " 字节）");
            return;
        }
        if (isConflict(modelId)) {
            replyStart(player, 1, "");
            return;
        }
        if (SESSIONS.size() >= MAX_CONCURRENT_SESSIONS) {
            replyStart(player, 4, "");
            return;
        }
        long uploadId = NEXT_UPLOAD_ID.incrementAndGet();
        SESSIONS.put(uploadId, new UploadSession(player, modelId, totalBytes, sha256Hex.toLowerCase(Locale.ROOT)));
        SESSION_BY_PLAYER.put(playerId, uploadId);
        NetworkHandler.sendToClientPlayer(new S2CModelUploadStartPacket(uploadId, (byte) 0, CHUNK_SIZE, maxTotalBytes, ServerConfig.UPLOAD_CHUNKS_PER_TICK.get(), ""), player);
    }

    public static void onChunk(ServerPlayer player, long uploadId, int offset, byte[] data) {
        if (data == null || data.length <= 0) {
            return;
        }
        UploadSession session = SESSIONS.get(uploadId);
        if (session == null || !session.is(player)) {
            return;
        }
        if (offset < 0 || offset + data.length > session.totalBytes) {
            return;
        }
        session.receive(offset, data);
    }

    public static void onFinish(ServerPlayer player, long uploadId) {
        UploadSession session = SESSIONS.get(uploadId);
        if (session == null || !session.is(player)) {
            if (uploadId > 0) {
                replyResult(player, uploadId, 4, "", "Session expired");
            }
            return;
        }
        // 摘除会话（完成或失败都不再接受后续片段）。
        SESSIONS.remove(uploadId);
        SESSION_BY_PLAYER.remove(player.getUUID(), uploadId);
        if (session.receivedChunks() != session.expectedChunkCount) {
            YesSteveModel.LOGGER.warn("[YSM] Upload {} for '{}' incomplete: {}/{} chunks", uploadId, session.modelId, session.receivedChunks(), session.expectedChunkCount);
            replyResult(player, uploadId, 5, "", "Incomplete upload");
            return;
        }
        if (!DigestUtil.sha256Hex(session.buffer).equalsIgnoreCase(session.sha256)) {
            replyResult(player, uploadId, 1, "", "Hash mismatch");
            return;
        }
        byte[] data = session.buffer;
        String uploaderName = session.playerName;
        String modelId = session.modelId;
        YSMThreadPool.submitSync(() -> processUpload(player, uploadId, modelId, data, uploaderName));
    }

    // ---------- 校验、落盘与重载（YSM Sync 线程） ----------

    private static void processUpload(ServerPlayer player, long uploadId, String modelId, byte[] data, String uploaderName) {
        String finalId = modelId + ".ysm";
        try {
            // 干跑解析：与 ServerModelManager#processYsmFile 同链，失败直接拒绝、不落盘。
            int ysmCryptoVersion = YesModelUtils.getYsmCryptoVersion(data);
            if (ysmCryptoVersion == -1) {
                throw new IllegalStateException("Unknown YSM crypto version");
            }
            if (ysmCryptoVersion == 1 || ysmCryptoVersion == 2) {
                Map<String, byte[]> input = YesModelUtils.input(data);
                try (YSMFolderDeserializer deserializer = new YSMFolderDeserializer(input)) {
                    deserializer.deserialize();
                }
            } else {
                byte[] decrypted = YsmCrypt.decryptYsmFile(data);
                try (YSMBinaryDeserializer deserializer = new YSMBinaryDeserializer(decrypted)) {
                    RawYsmModel rawModel = deserializer.deserializeKeepOpen();
                    deserializer.parseYSMFooter(rawModel);
                }
            }
        } catch (Throwable t) {
            YesSteveModel.LOGGER.warn("[YSM] Uploaded model '{}' failed to parse", modelId, t);
            replyResult(player, uploadId, 2, "", "Model failed to parse on server");
            return;
        }

        // 目标路径沙箱：合并后的真实路径必须仍落在 custom 目录内；不合法字符由 Path 解析抛
        // InvalidPathException，不做任何静态字符拦截（文件系统支持即接受）。
        Path customRoot = ServerModelManager.CUSTOM.toAbsolutePath().normalize();
        Path dest;
        try {
            dest = customRoot.resolve(finalId).normalize();
        } catch (InvalidPathException e) {
            YesSteveModel.LOGGER.warn("[YSM] Uploaded model '{}' rejected by filesystem", modelId, e);
            replyResult(player, uploadId, 6, "", "服务器文件系统无法表示该文件名: " + e.getReason());
            return;
        }
        if (!customRoot.equals(dest.getParent())) {
            YesSteveModel.LOGGER.warn("[YSM] Upload target for '{}' escapes custom dir: {}", modelId, dest);
            replyResult(player, uploadId, 6, "", "模型 ID 不能包含路径分隔符");
            return;
        }

        // CREATE_NEW 原子创建：与冲突检测之间的竞态也不静默覆盖既有模型。
        try {
            Files.createDirectories(customRoot);
            Files.write(dest, data, StandardOpenOption.CREATE_NEW);
            YesSteveModel.LOGGER.info("[YSM] Uploaded model '{}' ({} bytes) from player {} -> {}", modelId, data.length, uploaderName, dest);
        } catch (FileAlreadyExistsException e) {
            replyResult(player, uploadId, 1, "", "同名模型已存在");
            return;
        } catch (InvalidPathException e) {
            YesSteveModel.LOGGER.warn("[YSM] Upload target for '{}' rejected by filesystem", modelId, e);
            replyResult(player, uploadId, 6, "", "服务器文件系统无法表示该文件名: " + e.getReason());
            return;
        } catch (IOException e) {
            YesSteveModel.LOGGER.warn("[YSM] Upload write failed for model '{}'", modelId, e);
            replyResult(player, uploadId, 3, "", "服务器写入失败: " + e);
            return;
        }

        boolean scheduled = ServerModelManager.loadModels(loadResult -> {
            boolean ok = loadResult.isSuccess() && ServerModelManager.getServerModelInfo().containsKey(finalId);
            if (!ok) {
                YesSteveModel.LOGGER.warn("[YSM] Uploaded model '{}' was written but did not register", finalId);
                deleteFile(finalId);
                replyResult(player, uploadId, 2, "", "Model failed to load on server");
                return;
            }
            YesSteveModel.LOGGER.info("[YSM] Uploaded model '{}' is now available as '{}'", modelId, finalId);
            replyResult(player, uploadId, 0, finalId, "");
        }, null);
        if (!scheduled) {
            YesSteveModel.LOGGER.warn("[YSM] Model reload already in progress, upload of '{}' rejected", modelId);
            deleteFile(finalId);
            replyResult(player, uploadId, 6, "", "Server reload in progress, try again later");
        }
    }

    private static void deleteFile(String finalId) {
        Path dest = resolveTargetQuiet(finalId);
        if (dest == null) {
            return;
        }
        try {
            Files.deleteIfExists(dest);
        } catch (IOException e) {
            YesSteveModel.LOGGER.warn("[YSM] Failed to remove rejected upload file '{}'", finalId, e);
        }
    }

    private static String targetFileName(String modelId) {
        return modelId + ".ysm";
    }

    /** 解析 custom 下的目标路径；解析失败（如含 NUL）返回 null，不做静态拦截。 */
    private static Path resolveTargetQuiet(String finalId) {
        try {
            return ServerModelManager.CUSTOM.resolve(finalId);
        } catch (InvalidPathException e) {
            return null;
        }
    }

    /** 冲突判定：server 已登记的模型 ID（忽略大小写）+ custom 目录内的同名文件（忽略大小写）。 */
    private static boolean isConflict(String modelId) {
        String target = targetFileName(modelId);
        for (String existing : ServerModelManager.getServerModelInfo().keySet()) {
            if (existing.equalsIgnoreCase(modelId) || existing.equalsIgnoreCase(target)) {
                return true;
            }
        }
        Path customDir = resolveTargetQuiet("") != null ? ServerModelManager.CUSTOM : null;
        if (customDir == null || !Files.isDirectory(customDir)) {
            return false;
        }
        try (Stream<Path> files = Files.list(customDir)) {
            return files.anyMatch(p -> p.getFileName().toString().equalsIgnoreCase(target));
        } catch (IOException | InvalidPathException e) {
            return false; // 目录读取失败时放行，由写入阶段的 CREATE_NEW 原子仲裁兜底
        }
    }

    // ---------- 回包 ----------

    private static void replyStart(ServerPlayer player, int status, String message) {
        NetworkHandler.sendToClientPlayer(new S2CModelUploadStartPacket(0L, (byte) status, CHUNK_SIZE, ServerConfig.MAX_UPLOAD_BYTES.get(), ServerConfig.UPLOAD_CHUNKS_PER_TICK.get(), message), player);
    }

    /** 错误码语义见客户端 getResponseErrorText：1=已存在、2=解析失败、3=存储错误、4=会话过期、5=不完整、6=拒绝写入。 */
    private static void replyResult(ServerPlayer player, long uploadId, int status, String modelId, String message) {
        if (player.hasDisconnected()) {
            return;
        }
        NetworkHandler.sendToClientPlayer(new S2CModelUploadResultPacket(uploadId, (byte) status, modelId, 0L, 0L, message), player);
    }

    // ---------- 会话生命周期 ----------

    private static void tick() {
        if (SESSIONS.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        SESSIONS.entrySet().removeIf(entry -> {
            UploadSession session = entry.getValue();
            if (now - session.lastActivityMs > SESSION_TIMEOUT_MS) {
                expire(entry.getKey(), session, "timeout");
                return true;
            }
            return false;
        });
    }

    private static void expire(long uploadId, UploadSession session, String reason) {
        SESSION_BY_PLAYER.remove(session.playerId, uploadId);
        if (session.locked) {
            return; // 已进入完成/终止流程，不再推送
        }
        if (session.player.hasDisconnected()) {
            return;
        }
        YesSteveModel.LOGGER.warn("[YSM] Upload session {} for '{}' aborted: {}", uploadId, session.modelId, reason);
        replyResult(session.player, uploadId, 4, "", "Session expired");
    }

    private static void removePlayer(UUID playerId) {
        Long uploadId = SESSION_BY_PLAYER.remove(playerId);
        if (uploadId == null) {
            return;
        }
        SESSIONS.remove(uploadId);
    }

    /** 上传会话。buffer 按会话声明总量预分配（默认上限 16MB）。 */
    private static final class UploadSession {
        final ServerPlayer player;
        final UUID playerId;
        final String playerName;
        final String modelId;
        final int totalBytes;
        final String sha256;
        final byte[] buffer;
        final int expectedChunkCount;
        volatile long lastActivityMs = System.currentTimeMillis();
        volatile boolean locked;
        private final boolean[] received;
        private int receivedChunks;

        UploadSession(ServerPlayer player, String modelId, int totalBytes, String sha256) {
            this.player = player;
            this.playerId = player.getUUID();
            this.playerName = player.getGameProfile().name();
            this.modelId = modelId;
            this.totalBytes = totalBytes;
            this.sha256 = sha256;
            this.buffer = new byte[totalBytes];
            this.expectedChunkCount = (totalBytes + CHUNK_SIZE - 1) / CHUNK_SIZE;
            this.received = new boolean[expectedChunkCount];
        }

        boolean is(ServerPlayer sender) {
            return sender != null && sender.getUUID().equals(playerId);
        }

        int receivedChunks() {
            synchronized (this) {
                return receivedChunks;
            }
        }

        synchronized void receive(int offset, byte[] data) {
            if (locked) {
                return;
            }
            System.arraycopy(data, 0, buffer, offset, data.length);
            int chunkIndex = offset / CHUNK_SIZE;
            if (chunkIndex >= 0 && chunkIndex < received.length && !received[chunkIndex]) {
                received[chunkIndex] = true;
                receivedChunks++;
            }
            lastActivityMs = System.currentTimeMillis();
        }
    }
}
