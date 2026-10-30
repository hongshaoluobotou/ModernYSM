package rip.ysm.api.impl;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import rip.ysm.api.event.CommandEvents;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Fabric 侧的事件挂接实现（common 的 {@code rip.ysm.api.event.*} 抽象在此落地为 fabric-api 事件）。
 *
 * <p>与 1.20.1 的对应关系：
 * <ul>
 *   <li>{@code ServerLifecycleEvents.SERVER_STARTING} ← Architectury {@code LifecycleEvent.SERVER_BEFORE_START}</li>
 *   <li>{@code ServerPlayConnectionEvents.JOIN/DISCONNECT} ← {@code PlayerEvent.PLAYER_JOIN/PLAYER_LOGOUT}</li>
 *   <li>{@code ServerEntityEvents.ENTITY_LOAD} ← {@code EntityEvent.ADD}</li>
 *   <li>{@code ClientEntityEvents.ENTITY_LOAD} ← {@code EntityEvent.ADD}（客户端侧）</li>
 * </ul>
 */
public final class EventBusImpl {

    private EventBusImpl() {
    }

    // ---------------- 服务端 ----------------

    public static void onServerStarting(Consumer<MinecraftServer> callback) {
        ServerLifecycleEvents.SERVER_STARTING.register(callback::accept);
    }

    public static void onServerStopped(Consumer<MinecraftServer> callback) {
        ServerLifecycleEvents.SERVER_STOPPED.register(callback::accept);
    }

    public static void onServerTick(Consumer<MinecraftServer> callback) {
        ServerTickEvents.END_SERVER_TICK.register(callback::accept);
    }

    public static void onServerPlayerJoin(Consumer<ServerPlayer> callback) {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> callback.accept(handler.getPlayer()));
    }

    public static void onServerPlayerDisconnect(Consumer<ServerPlayer> callback) {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> callback.accept(handler.getPlayer()));
    }

    public static void onServerEntityJoin(BiConsumer<Entity, ServerLevel> callback) {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> callback.accept(entity, level));
    }

    // ---------------- 客户端 ----------------

    public static void onClientTick(Runnable callback) {
        ClientTickEvents.END_CLIENT_TICK.register(client -> callback.run());
    }

    public static void onClientTickStart(Runnable callback) {
        ClientTickEvents.START_CLIENT_TICK.register(client -> callback.run());
    }

    public static void onClientEntityLoad(BiConsumer<Entity, ClientLevel> callback) {
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> callback.accept(entity, level));
    }

    public static void onClientPlayJoin(Runnable callback) {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> callback.run());
    }

    public static void onClientPlayDisconnect(Runnable callback) {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> callback.run());
    }

    // ---------------- 命令 ----------------

    public static void onRegisterCommands(CommandEvents.CommandRegistrar registrar) {
        CommandRegistrationCallback.EVENT.register(registrar::register);
    }
}
