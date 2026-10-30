package rip.ysm.api.network.impl;

import com.elfmcys.yesstevemodel.mixin.ConnectionAccessor;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.C2SModelSyncPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import rip.ysm.api.PlatformAPI;
import rip.ysm.api.network.PacketContext;
import rip.ysm.api.network.PacketDirection;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Forge 侧的网络通道实现。
 *
 * <p>Forge 26.3 是「经典 SimpleChannel + 自定义 payload」体系（与 NeoForge 的
 * {@code PayloadRegistrar} 完全不同）：
 * <ul>
 *   <li>注册：{@code ChannelBuilder.named(id).networkProtocolVersion(1).simpleChannel()
 *       .any(flow -> flow.add(YsmRawPayload.class, CODEC, handler)).build()}；</li>
 *   <li>收发：{@code channel.send(msg, PacketDistributor.X.with/noArg())}，
 *       构造原生包用 {@code NetworkDirection.PLAY_TO_CLIENT.buildPacket(channel, msg)}；</li>
 *   <li>监听器回调：{@code CustomPayloadEvent.Context} 自带
 *       {@code isClientSide()/getSender()/getConnection()/enqueueWork()}，同一个 handler
 *       服务两个方向（不像 NeoForge 需要按方向注册两个）。</li>
 * </ul>
 *
 * <p>分片逻辑与 fabric/neoforge 版完全一致；Forge 的单包上限足够容纳 30KB 分片。
 */
public final class YSMChannelImpl {

    private static final int FRAGMENT_DISCRIMINATOR = 255;
    private static final int FRAGMENT_DATA_SIZE = 30_000;
    private static final int MAX_FRAGMENT_COUNT = 128;
    private static final int MAX_REASSEMBLED_SIZE = 2 * 1024 * 1024;
    private static final long FRAGMENT_TIMEOUT_NANOS = 30_000_000_000L;

    private static final Map<Integer, Codec<?>> CODECS_BY_ID = new HashMap<>();
    private static final Map<Class<?>, Integer> ID_BY_CLASS = new HashMap<>();
    private static final Map<Connection, Map<Integer, FragmentAccumulator>> INCOMING_FRAGMENTS = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT_TRANSFER_ID = new AtomicInteger();

    private static Identifier channelId;
    private static Channel<Object> channel;

    private YSMChannelImpl() {
    }

    public static void init(Identifier id, String version) {
        channelId = id;
        YsmRawPayload.init(channelId);
        // 构建链：any()（不限协议）→ flow(null)（不限方向，同一 handler 服务双向）→ add → build
        channel = ChannelBuilder.named(channelId)
                .networkProtocolVersion(1)
                .simpleChannel()
                .any()
                .flow((net.minecraft.network.protocol.PacketFlow) null)
                .add(YsmRawPayload.class, YsmRawPayload.CODEC,
                        (payload, context) -> {
                            // 必须标记为已处理：否则 Forge 会继续把同一个 payload 交给原版
                            // ClientPacketListener#handleCustomPayload（未知 payload 分支），
                            // 那里缓冲区已被本处理器读空 → 每秒刷 "Received empty payload" +
                            // "Unknown custom packet payload" 两条报错（2026.11.01 实测）。
                            context.setPacketHandled(true);
                            dispatch(new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data())),
                                    new ForgePayloadContext(context));
                        })
                .build();
    }

    public static <T> void register(int discriminator, Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, PacketContext> handler, PacketDirection direction) {
        if ((discriminator & ~0xff) != 0) {
            throw new IllegalArgumentException("Discriminator must fit in an unsigned byte (0-255): " + discriminator);
        }
        Codec<T> codec = new Codec<>(type, encoder, decoder, handler);
        CODECS_BY_ID.put(discriminator & 0xff, codec);
        ID_BY_CLASS.put(type, discriminator & 0xff);
    }

    public static void dispatch(FriendlyByteBuf buf, PacketContext ctx) {
        int discriminator = buf.readUnsignedByte();
        if (discriminator == FRAGMENT_DISCRIMINATOR) {
            handleFragment(FragmentPacket.decode(buf), ctx);
            return;
        }
        Codec<?> codec = CODECS_BY_ID.get(discriminator);
        if (codec != null) {
            codec.dispatch(buf, ctx);
        }
    }

    public static void sendToServer(Object packet) {
        if (PlatformAPI.isServer()) {
            return;
        }
        FriendlyByteBuf encoded = encode(packet);
        if (packet instanceof C2SModelSyncPayload
                && NetworkHandler.serverSupportsModelSyncFragments()
                && encoded.readableBytes() > FRAGMENT_DATA_SIZE) {
            byte[] data = copyAndRelease(encoded);
            sendFragments(data);
            return;
        }
        channel.send(wrap(encoded), PacketDistributor.SERVER.noArg());
    }

    public static void sendToClientPlayer(Object packet, ServerPlayer player) {
        channel.send(wrap(encode(packet)), PacketDistributor.PLAYER.with(player));
    }

    public static void sendToAll(Object packet) {
        channel.send(wrap(encode(packet)), PacketDistributor.ALL.noArg());
    }

    public static void sendToTrackingEntity(Object packet, Entity entity) {
        channel.send(wrap(encode(packet)), PacketDistributor.TRACKING_ENTITY.with(entity));
    }

    public static void sendToTrackingEntityAndSelf(Object packet, Player player) {
        channel.send(wrap(encode(packet)), PacketDistributor.TRACKING_ENTITY_AND_SELF.with(player));
    }

    public static Packet<?> toClientboundPacket(Object packet) {
        return NetworkDirection.PLAY_TO_CLIENT.buildPacket(channel, wrap(encode(packet)));
    }

    public static List<Packet<?>> toClientboundPackets(Object packet) {
        byte[] encoded = copyAndRelease(encode(packet));
        if (encoded.length <= FRAGMENT_DATA_SIZE) {
            return List.of(NetworkDirection.PLAY_TO_CLIENT.buildPacket(channel, new YsmRawPayload(encoded)));
        }
        if (encoded.length > MAX_REASSEMBLED_SIZE) {
            throw new IllegalArgumentException("Fragmented YSM packet exceeds maximum size");
        }
        List<Packet<?>> packets = new ArrayList<>();
        int transferId = NEXT_TRANSFER_ID.incrementAndGet();
        int fragmentCount = (encoded.length + FRAGMENT_DATA_SIZE - 1) / FRAGMENT_DATA_SIZE;
        for (int index = 0; index < fragmentCount; index++) {
            int from = index * FRAGMENT_DATA_SIZE;
            int to = Math.min(from + FRAGMENT_DATA_SIZE, encoded.length);
            FriendlyByteBuf fragment = new FriendlyByteBuf(Unpooled.buffer());
            fragment.writeByte(FRAGMENT_DISCRIMINATOR);
            FragmentPacket.encode(new FragmentPacket(transferId, index, fragmentCount, Arrays.copyOfRange(encoded, from, to)), fragment);
            packets.add(NetworkDirection.PLAY_TO_CLIENT.buildPacket(channel, wrap(fragment)));
        }
        return packets;
    }

    public static Packet<?> toServerboundPacket(Object packet) {
        if (PlatformAPI.isServer()) {
            throw new IllegalStateException("toServerboundPacket can only be invoked from the client environment");
        }
        return NetworkDirection.PLAY_TO_SERVER.buildPacket(channel, wrap(encode(packet)));
    }

    private static FriendlyByteBuf encode(Object packet) {
        Integer id = ID_BY_CLASS.get(packet.getClass());
        if (id == null) {
            throw new IllegalStateException("Packet type not registered: " + packet.getClass());
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeByte(id & 0xff);
        CODECS_BY_ID.get(id).encode(packet, buf);
        return buf;
    }

    private static YsmRawPayload wrap(FriendlyByteBuf buf) {
        try {
            byte[] data = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), data);
            return new YsmRawPayload(data);
        } finally {
            buf.release();
        }
    }

    private static byte[] copyAndRelease(FriendlyByteBuf buf) {
        try {
            byte[] data = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), data);
            return data;
        } finally {
            buf.release();
        }
    }

    private static void sendFragments(byte[] encoded) {
        if (encoded.length > MAX_REASSEMBLED_SIZE) {
            throw new IllegalArgumentException("Fragmented YSM packet exceeds maximum size");
        }
        int transferId = NEXT_TRANSFER_ID.incrementAndGet();
        int fragmentCount = (encoded.length + FRAGMENT_DATA_SIZE - 1) / FRAGMENT_DATA_SIZE;
        for (int index = 0; index < fragmentCount; index++) {
            int from = index * FRAGMENT_DATA_SIZE;
            int to = Math.min(from + FRAGMENT_DATA_SIZE, encoded.length);
            FriendlyByteBuf fragment = new FriendlyByteBuf(Unpooled.buffer());
            fragment.writeByte(FRAGMENT_DISCRIMINATOR);
            FragmentPacket.encode(new FragmentPacket(
                    transferId, index, fragmentCount, Arrays.copyOfRange(encoded, from, to)
            ), fragment);
            channel.send(wrap(fragment), PacketDistributor.SERVER.noArg());
        }
    }

    private static void handleFragment(FragmentPacket packet, PacketContext context) {
        long now = System.nanoTime();
        Connection connection = context.getConnection();
        Map<Integer, FragmentAccumulator> newTransfers = new ConcurrentHashMap<>();
        Map<Integer, FragmentAccumulator> transfers = INCOMING_FRAGMENTS.putIfAbsent(connection, newTransfers);
        if (transfers == null) {
            transfers = newTransfers;
            Map<Integer, FragmentAccumulator> registeredTransfers = transfers;
            ((ConnectionAccessor) connection).ysm$getChannel().closeFuture()
                    .addListener(ignored -> INCOMING_FRAGMENTS.remove(connection, registeredTransfers));
        }
        transfers.entrySet().removeIf(entry -> now - entry.getValue().lastUpdateNanos > FRAGMENT_TIMEOUT_NANOS);

        FragmentAccumulator accumulator = transfers.computeIfAbsent(
                packet.transferId(), ignored -> new FragmentAccumulator(packet.fragmentCount())
        );
        byte[] complete = accumulator.add(packet, now);
        if (complete == null) {
            return;
        }
        transfers.remove(packet.transferId(), accumulator);
        if (transfers.isEmpty()) {
            INCOMING_FRAGMENTS.remove(connection, transfers);
        }

        FriendlyByteBuf original = new FriendlyByteBuf(Unpooled.wrappedBuffer(complete));
        try {
            dispatch(original, context);
            if (original.isReadable()) {
                throw new IllegalArgumentException("Fragmented YSM packet left " + original.readableBytes() + " unread bytes");
            }
        } finally {
            original.release();
        }
    }

    private record FragmentPacket(int transferId, int fragmentIndex, int fragmentCount, byte[] data) {
        private static void encode(FragmentPacket packet, FriendlyByteBuf buf) {
            buf.writeVarInt(packet.transferId());
            buf.writeVarInt(packet.fragmentIndex());
            buf.writeVarInt(packet.fragmentCount());
            buf.writeByteArray(packet.data());
        }

        private static FragmentPacket decode(FriendlyByteBuf buf) {
            return new FragmentPacket(
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readByteArray(FRAGMENT_DATA_SIZE)
            );
        }
    }

    private static final class FragmentAccumulator {
        private final byte[][] fragments;
        private int received;
        private int totalSize;
        private volatile long lastUpdateNanos = System.nanoTime();

        private FragmentAccumulator(int fragmentCount) {
            if (fragmentCount <= 0 || fragmentCount > MAX_FRAGMENT_COUNT) {
                throw new IllegalArgumentException("Invalid YSM fragment count: " + fragmentCount);
            }
            this.fragments = new byte[fragmentCount][];
        }

        private synchronized byte[] add(FragmentPacket packet, long now) {
            if (packet.fragmentCount() != fragments.length
                    || packet.fragmentIndex() < 0
                    || packet.fragmentIndex() >= fragments.length) {
                throw new IllegalArgumentException("Inconsistent YSM fragment metadata");
            }
            lastUpdateNanos = now;
            if (fragments[packet.fragmentIndex()] == null) {
                fragments[packet.fragmentIndex()] = packet.data();
                received++;
                totalSize += packet.data().length;
                if (totalSize > MAX_REASSEMBLED_SIZE) {
                    throw new IllegalArgumentException("Fragmented YSM packet exceeds maximum size");
                }
            }
            if (received != fragments.length) {
                return null;
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream(totalSize);
            for (byte[] fragment : fragments) {
                output.write(fragment, 0, fragment.length);
            }
            return output.toByteArray();
        }
    }
}
