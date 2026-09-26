package rip.ysm.gpu;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/**
 * 26.3 port: 原 BlurStack 依赖裸 GL20 shader（BlurShader）+ 已删除的 GuiGraphics，
 * 已改写为 26.3 GuiRenderState 原生模糊的适配层：
 * flush() 时调用 GuiGraphicsExtractor#blurBeforeThisStratum()，
 * 由 vanilla 在该 stratum 之前对已绘制内容做全屏模糊。
 * 与原实现的差异：不再支持逐区域圆角/扇形遮罩与 tint（TODO 按需用 shader 重做）。
 */
public final class BlurStack {
    private static final List<Region> regions = new ArrayList<>();

    private static final class Region {
        boolean isPie;
        float x, y, w, h;
    }

    private BlurStack() {
    }

    public static void pushBlur(float x, float y, float w, float h, float cornerRadius, float blurRadius) {
        pushBlur(x, y, w, h, cornerRadius, blurRadius, 0xFFFFFFFF);
    }

    public static void pushBlur(float x, float y, float w, float h, float cornerRadius, float blurRadius, int tintRgba) {
        Region r = new Region();
        r.isPie = false;
        r.x = x;
        r.y = y;
        r.w = w;
        r.h = h;
        regions.add(r);
    }

    public static void pushBlurPie(float centerX, float centerY, float innerRadius, float outerRadius, float startAngle, float endAngle, float blurRadius) {
        pushBlurPie(centerX, centerY, innerRadius, outerRadius, startAngle, endAngle, blurRadius, 0xFFFFFFFF);
    }

    public static void pushBlurPie(float centerX, float centerY, float innerRadius, float outerRadius, float startAngle, float endAngle, float blurRadius, int tintRgba) {
        float pad = 1.0f;
        Region r = new Region();
        r.isPie = true;
        r.x = centerX - outerRadius - pad;
        r.y = centerY - outerRadius - pad;
        r.w = (outerRadius + pad) * 2.0f;
        r.h = (outerRadius + pad) * 2.0f;
        regions.add(r);
    }

    public static void popBlur() {
        if (!regions.isEmpty()) regions.remove(regions.size() - 1);
    }

    public static void clear() {
        regions.clear();
    }

    public static boolean isEmpty() {
        return regions.isEmpty();
    }

    public static void flush(GuiGraphicsExtractor graphics) {
        if (regions.isEmpty()) return;
        regions.clear();
        // 26.3：请求在该 stratum 之前对已提交的 GUI 内容执行 vanilla 全屏模糊。
        // 一帧只允许一次 blur；setScreenAndShow 触发的立即渲染帧等场景下 blur 名额可能已被
        // vanilla extractBackground 消耗，此时降级为不做背景模糊（不能让 GUI 崩溃）。
        try {
            graphics.blurBeforeThisStratum();
        } catch (IllegalStateException ignored) {
        }
    }
}
