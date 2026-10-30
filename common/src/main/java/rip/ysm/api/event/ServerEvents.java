package rip.ysm.api.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 服务端生命周期事件的加载器无关入口。
 *
 * <p>common 只负责「声明要监听什么」，具体挂接由各加载器模块的
 * {@code rip.ysm.api.impl.EventBusImpl} 完成（Fabric 走 fabric-api 事件，
 * NeoForge/Forge 走各自的事件总线）。
 *
 * <p>命名对应 Fabric 事件，便于与 1.20.1 的 Architectury 写法对照：
 * <ul>
 *   <li>{@link #onStarting} → {@code ServerLifecycleEvents.SERVER_STARTING}</li>
 *   <li>{@link #onStopped} → {@code ServerLifecycleEvents.SERVER_STOPPED}</li>
 *   <li>{@link #onTick} → {@code ServerTickEvents.END_SERVER_TICK}</li>
 *   <li>{@link #onPlayerJoin} → {@code ServerPlayConnectionEvents.JOIN}</li>
 *   <li>{@link #onPlayerDisconnect} → {@code ServerPlayConnectionEvents.DISCONNECT}</li>
 *   <li>{@link #onEntityJoinLevel} → {@code ServerEntityEvents.ENTITY_LOAD}</li>
 * </ul>
 */
public final class ServerEvents {

    private ServerEvents() {
    }

    /** 服务器启动中（此时世界尚未加载完成）。 */
    public static void onStarting(Consumer<MinecraftServer> callback) {
        rip.ysm.api.impl.EventBusImpl.onServerStarting(callback);
    }

    /** 服务器已停止。 */
    public static void onStopped(Consumer<MinecraftServer> callback) {
        rip.ysm.api.impl.EventBusImpl.onServerStopped(callback);
    }

    /** 服务器 tick 结束。 */
    public static void onTick(Consumer<MinecraftServer> callback) {
        rip.ysm.api.impl.EventBusImpl.onServerTick(callback);
    }

    /** 玩家加入（连接完成）。 */
    public static void onPlayerJoin(Consumer<ServerPlayer> callback) {
        rip.ysm.api.impl.EventBusImpl.onServerPlayerJoin(callback);
    }

    /** 玩家断线。 */
    public static void onPlayerDisconnect(Consumer<ServerPlayer> callback) {
        rip.ysm.api.impl.EventBusImpl.onServerPlayerDisconnect(callback);
    }

    /** 实体进入服务端世界。 */
    public static void onEntityJoinLevel(BiConsumer<Entity, ServerLevel> callback) {
        rip.ysm.api.impl.EventBusImpl.onServerEntityJoin(callback);
    }
}
