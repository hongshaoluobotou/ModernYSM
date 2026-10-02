package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.mixin.ServerCommonPacketListenerImplAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import rip.ysm.api.network.PacketContext;
import rip.ysm.api.network.LatestRequestLimiter;

import java.util.Map;
import java.util.WeakHashMap;

public class C2SRequestExecuteMolangPacket {

    // 滑条可每游戏 tick 同步一次，并保留短暂突发；所有请求仍只能操作自己的模型。
    public static final int MAX_EXPRESSION_LENGTH = 8192;
    private static final Map<Connection, LatestRequestLimiter<String>> REQUEST_BUDGETS = new WeakHashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                synchronized (REQUEST_BUDGETS) {
                    LatestRequestLimiter<String> limiter = REQUEST_BUDGETS.get(connection(player));
                    if (limiter != null) {
                        String expression = limiter.poll(System.nanoTime());
                        if (expression != null && player.isAlive()) {
                            broadcast(player, expression);
                        }
                    }
                }
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            synchronized (REQUEST_BUDGETS) {
                REQUEST_BUDGETS.remove(connection(handler.player));
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (REQUEST_BUDGETS) {
                REQUEST_BUDGETS.clear();
            }
        });
    }

    private static Connection connection(ServerPlayer player) {
        return ((ServerCommonPacketListenerImplAccessor) player.connection).ysm$getConnection();
    }

    private final String animationName;

    private final int entityId;

    public C2SRequestExecuteMolangPacket(String str, int i) {
        this.animationName = str;
        this.entityId = i;
    }

    public static void encode(C2SRequestExecuteMolangPacket message, FriendlyByteBuf buf) {
        buf.writeUtf(message.animationName, MAX_EXPRESSION_LENGTH);
        buf.writeVarInt(message.entityId);
    }

    public static C2SRequestExecuteMolangPacket decode(FriendlyByteBuf buf) {
        return new C2SRequestExecuteMolangPacket(buf.readUtf(MAX_EXPRESSION_LENGTH), buf.readVarInt());
    }

    public static void handle(C2SRequestExecuteMolangPacket message, PacketContext ctx) {
        if (ctx.isServerSide()) {
            ctx.enqueueWork(() -> handleOnServer(message, ctx.getSender()));
        }
    }

    public static void handleOnServer(C2SRequestExecuteMolangPacket message, ServerPlayer sender) {
        // GUI/轮盘/滑条的合法目标都是发送者自身；恢复第三方实体兼容时必须显式校验其归属。
        if (sender == null || !sender.isAlive() || message.entityId != sender.getId()
                || message.animationName.isBlank() || message.animationName.length() > MAX_EXPRESSION_LENGTH) {
            return;
        }
        long now = System.nanoTime();
        synchronized (REQUEST_BUDGETS) {
            LatestRequestLimiter<String> budget = REQUEST_BUDGETS.computeIfAbsent(connection(sender),
                    ignored -> new LatestRequestLimiter<>(40, 20, now));
            String ready = budget.submit(message.animationName, now);
            if (ready != null) {
                broadcast(sender, ready);
            }
        }
    }

    private static void broadcast(ServerPlayer sender, String expression) {
        NetworkHandler.sendToTrackingEntity(new S2CExecuteMolangPacket(sender.getId(), expression), sender);
    }
}
