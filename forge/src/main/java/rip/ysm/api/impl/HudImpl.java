package rip.ysm.api.impl;

import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import rip.ysm.api.client.HudRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge 侧的 HUD 元素注册。
 *
 * <p>Forge 26.3 没有 NeoForge 的 {@code RegisterGuiLayersEvent}，取而代之的是
 * {@code AddGuiOverlayLayersEvent}（携带 {@code ForgeLayeredDraw}）：
 * 往根层末尾 {@code add(Identifier, ForgeLayer)} 即等价于 NeoForge 的 {@code registerAboveAll}
 * / fabric 的 {@code HudElementRegistry.addLast}。{@code ForgeLayer#extract(GuiGraphicsExtractor, DeltaTracker)}
 * 与本项目 {@link HudRenderer#extractRenderState} 签名一致。
 */
public final class HudImpl {

    private record Pending(Identifier id, HudRenderer renderer) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();
    private static boolean hooked;

    private HudImpl() {
    }

    public static void init() {
        if (hooked) {
            return;
        }
        hooked = true;
        AddGuiOverlayLayersEvent.BUS.addListener(event -> {
            for (Pending pending : PENDING) {
                event.getLayeredDraw().add(pending.id(),
                        (guiGraphics, deltaTracker) -> pending.renderer().extractRenderState(guiGraphics, deltaTracker));
            }
            PENDING.clear();
        });
    }

    public static void registerHudElement(Identifier id, HudRenderer renderer) {
        PENDING.add(new Pending(id, renderer));
    }
}
