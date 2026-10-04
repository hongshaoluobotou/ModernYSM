package rip.ysm.api.network.fabric;

import com.elfmcys.yesstevemodel.mixin.ConnectionAccessor;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.C2SModelSyncPayload;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import rip.ysm.api.network.PacketContext;
import rip.ysm.api.network.FragmentReassembler;
import rip.ysm.api.network.PacketDirection;
import rip.ysm.api.network.fabric.client.YSMChannelClientImpl;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class YSMChannelImpl {

    private static final int FRAGMENT_DISCRIMINATOR = 255;
    private static final int FRAGMENT_DATA_SIZE = 30_000;
    private static final int MAX_REASSEMBLED_SIZE = 2 * 1024 * 1024;

    private static final Map<Integer, Codec<?>> CODECS_BY_ID = new HashMap<>();
    private static final Map<Class<?>, Integer> ID_BY_CLASS = new HashMap<>();
    private static final FragmentReassembler<Connection> INCOMING_FRAGMENTS = new FragmentReassembler<>();
    private static final Set<Connection> FRAGMENT_CONNECTIONS = Collections.newSetFromMap(new WeakHashMap<>());
    private static long lastFragmentCleanup = System.nanoTime();
    private static final AtomicInteger NEXT_TRANSFER_ID = new AtomicInteger();

    private static Identifier channelId;
    private static volatile MinecraftServer currentServer;

    private YSMChannelImpl() {
    }

    public static void init(Identifier id, String version) {
        channelId = id;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> currentServer = server);
        ServerTickEvents.END_SERVER_TICK.register(server -> cleanupExpiredFragments());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> currentServer = null);

        // TODO port: 26.3 fabric-api payload 体系；TYPE + codec 注册（registerLarge 兼容大包/分片）
        YsmRawPayload.init(channelId);
        PayloadTypeRegistry.serverboundPlay().registerLarge(YsmRawPayload.TYPE, YsmRawPayload.CODEC, MAX_REASSEMBLED_SIZE);
        PayloadTypeRegistry.clientboundPlay().registerLarge(YsmRawPayload.TYPE, YsmRawPayload.CODEC, MAX_REASSEMBLED_SIZE);
        ServerPlayNetworking.registerGlobalReceiver(YsmRawPayload.TYPE, (payload, ctx) -> dispatch(new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(payload.data())), new ServerPacketContext(ctx.server(), ctx.player(), ctx.packetContext().orElseThrow(net.fabricmc.fabric.api.networking.v1.context.PacketContext.CONNECTION))));

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            YSMChannelClientImpl.init(channelId);
        }
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
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
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
        YSMChannelClientImpl.sendToServer(wrap(encoded));
    }

    public static void sendToClientPlayer(Object packet, ServerPlayer player) {
        ServerPlayNetworking.send(player, wrap(encode(packet)));
    }

    public static void sendToAll(Object packet) {
        MinecraftServer server = currentServer;
        if (server == null) {
            return;
        }
        for (ServerPlayer player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, wrap(encode(packet)));
        }
    }

    public static void sendToTrackingEntity(Object packet, Entity entity) {
        for (ServerPlayer player : PlayerLookup.tracking(entity)) {
            ServerPlayNetworking.send(player, wrap(encode(packet)));
        }
    }

    public static void sendToTrackingEntityAndSelf(Object packet, Player player) {
        for (ServerPlayer p : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(p, wrap(encode(packet)));
        }
        if (player instanceof ServerPlayer self) {
            ServerPlayNetworking.send(self, wrap(encode(packet)));
        }
    }

    public static Packet<?> toClientboundPacket(Object packet) {
        return ServerPlayNetworking.createClientboundPacket(wrap(encode(packet)));
    }

    public static List<Packet<?>> toClientboundPackets(Object packet) {
        byte[] encoded = copyAndRelease(encode(packet));
        if (encoded.length <= FRAGMENT_DATA_SIZE) {
            return List.of(ServerPlayNetworking.createClientboundPacket(new YsmRawPayload(encoded)));
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
            packets.add(ServerPlayNetworking.createClientboundPacket(wrap(fragment)));
        }
        return packets;
    }

    public static Packet<?> toServerboundPacket(Object packet) {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            throw new IllegalStateException("toServerboundPacket can only be invoked from the client environment");
        }
        return YSMChannelClientImpl.toServerboundPacket(wrap(encode(packet)));
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
            YSMChannelClientImpl.sendToServer(wrap(fragment));
        }
    }

    private static void handleFragment(FragmentPacket packet, PacketContext context) {
        Connection connection = context.getConnection();
        synchronized (FRAGMENT_CONNECTIONS) {
            if (FRAGMENT_CONNECTIONS.add(connection)) {
                ((ConnectionAccessor) connection).ysm$getChannel().closeFuture().addListener(ignored -> {
                    INCOMING_FRAGMENTS.removeConnection(connection);
                    synchronized (FRAGMENT_CONNECTIONS) {
                        FRAGMENT_CONNECTIONS.remove(connection);
                    }
                });
            }
        }
        byte[] complete = INCOMING_FRAGMENTS.accept(connection, packet.transferId(), packet.fragmentIndex(),
                packet.fragmentCount(), packet.data(), System.nanoTime());
        if (complete == null) {
            return;
        }
        // 分片只能包装普通数据包，禁止嵌套重组绕过配额和递归限制。
        if (complete.length == 0 || (complete[0] & 0xff) == FRAGMENT_DISCRIMINATOR) {
            throw new IllegalArgumentException("Nested or empty YSM fragment payload");
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
            buf.writeVarInt(packet.transferId);
            buf.writeVarInt(packet.fragmentIndex);
            buf.writeVarInt(packet.fragmentCount);
            buf.writeByteArray(packet.data);
        }

        private static FragmentPacket decode(FriendlyByteBuf buf) {
            return new FragmentPacket(
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readByteArray(FRAGMENT_DATA_SIZE)
            );
        }
    }

    // 服务端和客户端 tick 都调用；不依赖后续网络流量，最多每秒扫描一次有界表。
    public static synchronized void cleanupExpiredFragments() {
        long now = System.nanoTime();
        if (now - lastFragmentCleanup >= 1_000_000_000L) {
            lastFragmentCleanup = now;
            INCOMING_FRAGMENTS.expire(now);
        }
    }
}
