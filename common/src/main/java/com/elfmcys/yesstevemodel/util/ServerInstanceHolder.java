package com.elfmcys.yesstevemodel.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric 环境下的 GameInstance.getServer() 替代：
 * 通过 ServerLifecycleEvents 跟踪当前服务器实例。
 * 必须在模组初始化阶段调用 {@link #init()} 完成事件注册。
 */
public final class ServerInstanceHolder {

    @Nullable
    private static volatile MinecraftServer server;

    private ServerInstanceHolder() {
    }

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTING.register(s -> server = s);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> server = null);
    }

    @Nullable
    public static MinecraftServer getServer() {
        return server;
    }
}
