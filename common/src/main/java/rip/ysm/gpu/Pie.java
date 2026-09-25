package rip.ysm.gpu;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 26.3 port: 原 Pie 依赖裸 GL20 shader（PieShader）+ 已删除的 GuiGraphics，
 * 已改写为 GuiGraphicsExtractor 上的矩形近似绘制：
 * 沿圆环按角度采样，用小方块近似填充环形/扇形区域。
 * 与原实现的差异：无 feather 软边缘（参数保留但忽略）。
 * TODO 若需要平滑边缘，按 renderpearl 方式迁移到 RenderPipeline。
 */
public final class Pie {
    public static final float tau = (float) (Math.PI * 2.0);

    private static final int STEPS = 48;

    private Pie() {
    }

    public static void draw(GuiGraphicsExtractor graphics, float centerX, float centerY, float innerRadius, float outerRadius, float startAngle, float endAngle, int rgba) {
        draw(graphics, centerX, centerY, innerRadius, outerRadius, startAngle, endAngle, rgba, 1.0f);
    }

    public static void draw(GuiGraphicsExtractor graphics, float centerX, float centerY, float innerRadius, float outerRadius, float startAngle, float endAngle, int rgba, float feather) {
        float span = endAngle - startAngle;
        if (outerRadius <= 0.0f || span <= 0.0f) return;
        innerRadius = Math.max(0.0f, innerRadius);

        // 按弧长决定采样步数，保证每步约 2px
        float avgRadius = Math.max(1.0f, (innerRadius + outerRadius) * 0.5f);
        int steps = Math.max(4, Math.min(256, (int) Math.ceil(Math.abs(span) * avgRadius / 2.0f)));
        float stepAngle = span / steps;

        // 径向分层（内半径到外半径），每层按角度采样小方块
        float radialStep = 1.5f;
        int layers = Math.max(1, (int) Math.ceil((outerRadius - innerRadius) / radialStep));
        for (int i = 0; i < steps; i++) {
            float ang = startAngle + (i + 0.5f) * stepAngle;
            float cos = (float) Math.cos(ang);
            float sin = (float) Math.sin(ang);
            for (int l = 0; l < layers; l++) {
                float rad = innerRadius + (l + 0.5f) * (outerRadius - innerRadius) / layers;
                float px = centerX + cos * rad;
                float py = centerY + sin * rad;
                float half = Math.max(0.8f, stepAngle * rad * 0.75f);
                graphics.fill(Math.round(px - half), Math.round(py - half),
                        Math.round(px + half), Math.round(py + half), rgba);
            }
        }
    }
}
