package com.elfmcys.yesstevemodel.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 26.3 的 GuiGraphicsExtractor 不公开其内部 GuiRenderState，
 * 自定义几何元素（rip.ysm.gpu.Pie 的三角形扇）需要经 addGuiElement 提交，
 * 故用 accessor 暴露。
 */
@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsExtractorAccessor {
    @Accessor("guiRenderState")
    GuiRenderState ysm$getGuiRenderState();
}
