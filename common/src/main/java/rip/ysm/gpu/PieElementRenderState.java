package rip.ysm.gpu;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;

/**
 * 26.3 移植：环形/扇形（Pie）的真实几何提交。
 *
 * <p>原 1.20.1 的 Pie 是裸 GL20 shader 单 quad 绘制（软边缘 feather）；
 * 26.3 无自定义 shader 的 GUI 提交口，此前用 fill 矩形采样近似——
 * 一个轮盘切片要 ~1200 次 fill、整屏一圈上万矩形，真机严重掉帧。
 * 现改为自定义 {@link GuiElementRenderState}：走 vanilla {@code RenderPipelines.GUI}
 * （POSITION_COLOR、QUADS 拓扑）提交环带 quad（inner=0 退化为中心三角形），
 * 每切片仅十几段，每帧总量 <1000 顶点，与 1.20.1 的开销同量级。差异：无 shader feather 软边缘
 * （参数保留忽略），边缘为硬边。</p>
 */
record PieElementRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        float centerX,
        float centerY,
        float innerRadius,
        float outerRadius,
        float startAngle,
        float endAngle,
        int rgba,
        ScreenRectangle scissorArea
) implements GuiElementRenderState {

    @Override
    public void buildVertices(VertexConsumer consumer) {
        float span = endAngle - startAngle;
        if (span <= 0.0f || outerRadius <= 0.0f) {
            return;
        }
        // 每个四边形约覆盖 6px 弧长（外缘），上限 128 段
        int steps = Math.max(2, Math.min(128, (int) Math.ceil(span * outerRadius / 6.0f)));
        float stepAngle = span / steps;
        float inner = Math.max(0.0f, innerRadius);
        // 26.3 GUI 管线是 QUADS 拓扑且剔除背面：顶点绕序须与 vanilla fill 一致
        // （vanilla ColoredRectangleRenderState: (x0,y0),(x0,y1),(x1,y1),(x1,y0)，负向绕序）。
        // 每段环带发 (I0,I1,O1,O0)；inner=0 时退化为 (C,C,O1,O0) 三角形（重复中心顶点补满 quad）。
        for (int i = 0; i < steps; i++) {
            float a0 = startAngle + i * stepAngle;
            float a1 = a0 + stepAngle;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            float xO0 = centerX + outerRadius * c0;
            float yO0 = centerY + outerRadius * s0;
            float xO1 = centerX + outerRadius * c1;
            float yO1 = centerY + outerRadius * s1;
            if (inner <= 0.0f) {
                vertex(consumer, centerX, centerY);
                vertex(consumer, centerX, centerY);
                vertex(consumer, xO1, yO1);
                vertex(consumer, xO0, yO0);
            } else {
                float xI0 = centerX + innerRadius * c0;
                float yI0 = centerY + innerRadius * s0;
                float xI1 = centerX + innerRadius * c1;
                float yI1 = centerY + innerRadius * s1;
                vertex(consumer, xI0, yI0);
                vertex(consumer, xI1, yI1);
                vertex(consumer, xO1, yO1);
                vertex(consumer, xO0, yO0);
            }
        }
    }

    private void vertex(VertexConsumer consumer, float x, float y) {
        consumer.addVertexWith2DPose(pose, x, y).setColor(rgba);
    }

    @Override
    public ScreenRectangle bounds() {
        // 用局部包围盒四角经 2D pose 变换后的包围盒（vanilla ColoredRectangleRenderState 同思路）
        float r = Math.max(0.0f, outerRadius);
        float minX = centerX - r;
        float minY = centerY - r;
        float maxX = centerX + r;
        float maxY = centerY + r;
        float tx0 = Float.MAX_VALUE, ty0 = Float.MAX_VALUE, tx1 = -Float.MAX_VALUE, ty1 = -Float.MAX_VALUE;
        Vector2f v = new Vector2f();
        for (float[] p : new float[][]{{minX, minY}, {maxX, minY}, {minX, maxY}, {maxX, maxY}}) {
            pose.transformPosition(p[0], p[1], v);
            tx0 = Math.min(tx0, v.x);
            ty0 = Math.min(ty0, v.y);
            tx1 = Math.max(tx1, v.x);
            ty1 = Math.max(ty1, v.y);
        }
        int x0 = Mth2.floor(tx0);
        int y0 = Mth2.floor(ty0);
        return new ScreenRectangle(x0, y0, Mth2.ceil(tx1) - x0, Mth2.ceil(ty1) - y0);
    }

    /** 局部工具，避免整包引入 Mth 的依赖噪音。 */
    private static final class Mth2 {
        static int floor(float v) {
            int i = (int) v;
            return v < i ? i - 1 : i;
        }

        static int ceil(float v) {
            int i = (int) v;
            return v > i ? i + 1 : i;
        }
    }
}
