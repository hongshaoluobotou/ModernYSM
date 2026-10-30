package com.elfmcys.yesstevemodel.util;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import rip.ysm.api.event.ServerEvents;

/**
 * 加载器无关的「当前服务器实例」持有者（原 Fabric 版：
 * 用 {@code ServerLifecycleEvents} 跟踪 {@link MinecraftServer}）。
 *
 * <p>必须在模组初始化阶段调用 {@link #init()} 完成事件注册。
 */
public final class ServerInstanceHolder {

    @Nullable
    private static volatile MinecraftServer server;

    private static boolean initialized;

    private ServerInstanceHolder() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerEvents.onStarting(s -> server = s);
        ServerEvents.onStopped(s -> server = null);
    }

    @Nullable
    public static MinecraftServer getServer() {
        return server;
    }
}
