package com.elfmcys.yesstevemodel.neoforge;

import net.neoforged.bus.api.IEventBus;

/**
 * NeoForge mod 事件总线持有者。
 *
 * <p>common 侧的部分加载器实现（如 {@code rip.ysm.api.network.impl.YSMChannelImpl}）由
 * {@code YesSteveModel.init()} 调用，拿不到 {@code @Mod} 构造器参数，故由入口点先把总线存下来。
 */
public final class NeoForgeModBus {

    private static IEventBus modBus;

    private NeoForgeModBus() {
    }

    public static void set(IEventBus bus) {
        modBus = bus;
    }

    public static IEventBus get() {
        if (modBus == null) {
            throw new IllegalStateException("NeoForge mod event bus not initialized yet");
        }
        return modBus;
    }
}
