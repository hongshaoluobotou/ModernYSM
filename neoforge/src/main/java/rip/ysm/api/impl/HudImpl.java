package rip.ysm.api.impl;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import rip.ysm.api.client.HudRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * NeoForge 侧的 HUD 元素注册（{@code RegisterGuiLayersEvent#registerAboveAll}）。
 *
 * <p>与 fabric 版（{@code HudElementRegistry.addLast}）语义一致：追加到最上层。
 * 事件是 mod 总线事件，故先入队，由客户端入口点调用 {@link #init(IEventBus)} 后统一注册。
 */
public final class HudImpl {

    private record Pending(Identifier id, HudRenderer renderer) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();
    private static boolean hooked;

    private HudImpl() {
    }

    public static void init(IEventBus modBus) {
        if (hooked) {
            return;
        }
        hooked = true;
        modBus.addListener(RegisterGuiLayersEvent.class, event -> {
            for (Pending pending : PENDING) {
                GuiLayer layer = (guiGraphics, deltaTracker) ->
                        pending.renderer().extractRenderState(guiGraphics, deltaTracker);
                event.registerAboveAll(pending.id(), layer);
            }
            PENDING.clear();
        });
    }

    public static void registerHudElement(Identifier id, HudRenderer renderer) {
        PENDING.add(new Pending(id, renderer));
    }
}
