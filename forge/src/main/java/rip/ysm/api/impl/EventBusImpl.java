package rip.ysm.api.impl;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import rip.ysm.api.event.CommandEvents;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Forge 侧的事件挂接实现。
 *
 * <p>Forge 26.3 使用 EventBus 7.x 的「每个事件类一个 {@code BUS} 静态字段」模型：
 * 监听一律 {@code XxxEvent.BUS.addListener(consumer)}，不再有全局 {@code MinecraftForge.EVENT_BUS.addListener}
 * （后者已退化为兼容用的注解扫描包装）。事件本身都是 record，访问器就是组件名（如 {@code getServer()}）。
 */
public final class EventBusImpl {

    private EventBusImpl() {
    }

    // ---------------- 服务端 ----------------

    public static void onServerStarting(Consumer<MinecraftServer> callback) {
        ServerAboutToStartEvent.BUS.addListener(event -> callback.accept(event.getServer()));
    }

    public static void onServerStopped(Consumer<MinecraftServer> callback) {
        ServerStoppedEvent.BUS.addListener(event -> callback.accept(event.getServer()));
    }

    public static void onServerTick(Consumer<MinecraftServer> callback) {
        TickEvent.ServerTickEvent.Post.BUS.addListener(event -> callback.accept(event.server()));
    }

    public static void onServerPlayerJoin(Consumer<ServerPlayer> callback) {
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                callback.accept(player);
            }
        });
    }

    public static void onServerPlayerDisconnect(Consumer<ServerPlayer> callback) {
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                callback.accept(player);
            }
        });
    }

    public static void onServerEntityJoin(BiConsumer<Entity, ServerLevel> callback) {
        EntityJoinLevelEvent.BUS.addListener(event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                callback.accept(event.getEntity(), level);
            }
        });
    }

    // ---------------- 客户端 ----------------

    public static void onClientTick(Runnable callback) {
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> callback.run());
    }

    public static void onClientTickStart(Runnable callback) {
        TickEvent.ClientTickEvent.Pre.BUS.addListener(event -> callback.run());
    }

    public static void onClientEntityLoad(BiConsumer<Entity, ClientLevel> callback) {
        EntityJoinLevelEvent.BUS.addListener(event -> {
            if (event.getLevel() instanceof ClientLevel level) {
                callback.accept(event.getEntity(), level);
            }
        });
    }

    public static void onClientPlayJoin(Runnable callback) {
        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(event -> callback.run());
    }

    public static void onClientPlayDisconnect(Runnable callback) {
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> callback.run());
    }

    // ---------------- 命令 ----------------

    public static void onRegisterCommands(CommandEvents.CommandRegistrar registrar) {
        RegisterCommandsEvent.BUS.addListener(event ->
                registrar.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection()));
    }
}
