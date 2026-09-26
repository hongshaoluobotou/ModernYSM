package rip.ysm.gpu;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.joml.Matrix3x2f;

/**
 * 26.3 port: 原 Pie 依赖裸 GL20 shader（PieShader），26.3 GUI 走 GuiRenderState
 * 提交体系且不暴露自定义 shader 口。现改为经自定义 GuiElementRenderState
 * （{@link PieElementRenderState}，vanilla RenderPipelines.GUI 管线）提交真实
 * 三角形扇/环带几何——此前版本曾用 fill 矩形采样近似（万级矩形/帧），真机严重掉帧。
 * 与原实现的差异：无 feather 软边缘（参数保留但忽略）。
 */
public final class Pie {
    public static final float tau = (float) (Math.PI * 2.0);

    private static final RenderPipeline PIPELINE = RenderPipelines.GUI;

    private Pie() {
    }

    public static void draw(GuiGraphicsExtractor graphics, float centerX, float centerY, float innerRadius, float outerRadius, float startAngle, float endAngle, int rgba) {
        draw(graphics, centerX, centerY, innerRadius, outerRadius, startAngle, endAngle, rgba, 1.0f);
    }

    public static void draw(GuiGraphicsExtractor graphics, float centerX, float centerY, float innerRadius, float outerRadius, float startAngle, float endAngle, int rgba, float feather) {
        float span = endAngle - startAngle;
        if (outerRadius <= 0.0f || span <= 0.0f) return;

        GuiRenderState renderState = ((com.elfmcys.yesstevemodel.mixin.client.GuiGraphicsExtractorAccessor) graphics).ysm$getGuiRenderState();
        if (renderState == null) return;
        // NOTE: 26.3 当前 scissorStack 未通过 accessor 暴露；Pie 的调用点（轮盘/模型按钮进度环）均不在 scissor 内。
        // TODO 若将来需要在 scissor 内画 Pie，扩展 GuiGraphicsExtractorAccessor 暴露 scissorStack.peek()。
        renderState.addGuiElement(new PieElementRenderState(
                PIPELINE,
                TextureSetup.noTexture(),
                new Matrix3x2f(graphics.pose()),
                centerX,
                centerY,
                Math.max(0.0f, innerRadius),
                outerRadius,
                startAngle,
                endAngle,
                rgba,
                null
        ));
    }
}
