package rip.ysm.api.impl;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import rip.ysm.api.client.HudRenderer;

/**
 * Fabric 侧的 HUD 元素注册（{@code HudElementRegistry.addLast}）。
 */
public final class HudImpl {

    private HudImpl() {
    }

    public static void registerHudElement(Identifier id, HudRenderer renderer) {
        HudElementRegistry.addLast(id, (guiGraphics, deltaTracker) -> renderer.extractRenderState(guiGraphics, deltaTracker));
    }
}
