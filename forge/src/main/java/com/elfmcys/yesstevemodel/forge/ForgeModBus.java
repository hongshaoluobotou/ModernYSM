package com.elfmcys.yesstevemodel.forge;

import net.minecraftforge.eventbus.api.bus.BusGroup;

/**
 * Forge mod 事件总线组持有者。
 *
 * <p>Forge 26.3 的 EventBus 7 按事件类分总线（{@code XxxEvent.BUS}），绝大多数监听直接挂静态 BUS 即可；
 * 只有 {@code RegisterEvent}（注册表写入）需要 **mod BusGroup**（{@code RegisterEvent.getBus(modBusGroup)}），
 * 而 common 侧的注册发生在 {@code YesSteveModel.init()} 里、拿不到构造器参数，故由入口点先存下来。
 */
public final class ForgeModBus {

    private static BusGroup modBusGroup;

    private ForgeModBus() {
    }

    public static void set(BusGroup busGroup) {
        modBusGroup = busGroup;
    }

    public static BusGroup get() {
        if (modBusGroup == null) {
            throw new IllegalStateException("Forge mod bus group not initialized yet");
        }
        return modBusGroup;
    }
}
