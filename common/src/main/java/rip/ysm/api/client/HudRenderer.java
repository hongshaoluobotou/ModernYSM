package rip.ysm.api.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * 加载器无关的 HUD 元素（替代 Fabric API 的 {@code HudElement}）。
 *
 * <p>26.3 的 HUD 体系是「提取式」：每帧由 {@code GuiRenderState} 收集各元素，
 * 因此实现方只需往 {@code GuiGraphicsExtractor} 里写提取调用，不要直接发 GL 命令。
 */
@FunctionalInterface
public interface HudRenderer {

    void extractRenderState(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker);

    /**
     * 注册一个 HUD 元素（追加到最上层）。
     * Fabric：{@code HudElementRegistry.addLast}；NeoForge：{@code RenderGuiLayerEvent} / {@code RegisterGuiLayersEvent}。
     */
    static void register(Identifier id, HudRenderer renderer) {
        rip.ysm.api.impl.HudImpl.registerHudElement(id, renderer);
    }
}
