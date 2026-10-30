package rip.ysm.api.impl;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import rip.ysm.api.event.CommandEvents;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * NeoForge 侧的事件挂接实现（common 的 {@code rip.ysm.api.event.*} 抽象在此落地为 NeoForge EventBus）。
 *
 * <p>全部监听挂在游戏总线 {@code NeoForge.EVENT_BUS}（非 mod 总线），因此可以在任意时机注册。
 * 与 fabric 版的对应关系见 AGENTS.md 的「能力映射表」。
 */
public final class EventBusImpl {

    private EventBusImpl() {
    }

    // ---------------- 服务端 ----------------

    public static void onServerStarting(Consumer<MinecraftServer> callback) {
        NeoForge.EVENT_BUS.addListener(ServerAboutToStartEvent.class, event -> callback.accept(event.getServer()));
    }

    public static void onServerStopped(Consumer<MinecraftServer> callback) {
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> callback.accept(event.getServer()));
    }

    public static void onServerTick(Consumer<MinecraftServer> callback) {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> callback.accept(event.getServer()));
    }

    public static void onServerPlayerJoin(Consumer<ServerPlayer> callback) {
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                callback.accept(player);
            }
        });
    }

    public static void onServerPlayerDisconnect(Consumer<ServerPlayer> callback) {
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                callback.accept(player);
            }
        });
    }

    public static void onServerEntityJoin(BiConsumer<Entity, ServerLevel> callback) {
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                callback.accept(event.getEntity(), level);
            }
        });
    }

    // ---------------- 客户端 ----------------

    public static void onClientTick(Runnable callback) {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> callback.run());
    }

    public static void onClientTickStart(Runnable callback) {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Pre.class, event -> callback.run());
    }

    public static void onClientEntityLoad(BiConsumer<Entity, net.minecraft.client.multiplayer.ClientLevel> callback) {
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, event -> {
            if (event.getLevel() instanceof net.minecraft.client.multiplayer.ClientLevel level) {
                callback.accept(event.getEntity(), level);
            }
        });
    }

    public static void onClientPlayJoin(Runnable callback) {
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingIn.class, event -> callback.run());
    }

    public static void onClientPlayDisconnect(Runnable callback) {
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> callback.run());
    }

    // ---------------- 命令 ----------------

    public static void onRegisterCommands(CommandEvents.CommandRegistrar registrar) {
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class,
                event -> registrar.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection()));
    }
}
