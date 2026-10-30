package com.elfmcys.yesstevemodel.client.renderer.pip;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HUD / P 键纸娃娃的 PiP 区域自动收紧（2026.11.03，对应审查意见 #6）。
 *
 * <p><b>背景</b>：{@code renderPlayerOverlay} 的 region 就是 PiP 渲染目标纹理尺寸，同时也是硬裁剪边界。
 * 1.20.1 的纸娃娃<b>完全无 scissor</b>（唯一裁剪来自屏幕边缘），所以把 region 取成整个 GUI 才能逐像素等价
 * ——代价是每帧一张屏幕尺寸的颜色 + 深度纹理（4K 下约 33MB × 2）加一次整屏合成。</p>
 *
 * <p><b>做法</b>：不猜余量（按配置框加余量会裁掉攻击/施法动画伸出的武器，已被真机证伪两次），
 * 而是<b>实测</b>——渲染完一帧后，从 GPU 提交前记录的顶点坐标里读出模型实际占用的屏幕包围盒
 * （顶点在 {@code GeoReplacedEntityRenderer} 记录时已乘上完整 pose，落在 PiP 纹理像素空间，
 * 再按 region 原点与 guiScale 换算回绝对屏幕坐标），下一帧用
 * {@code 实测包围盒 ∪ 配置框} + margin 作为 region。</p>
 *
 * <p><b>不会重新引入裁剪</b>（两条规则保证收敛、不振荡）：</p>
 * <ol>
 *   <li>上一帧的实测包围盒贴到纹理边界（说明已被裁过、测量值只是下界）→ 下一帧把它再放大 1.5 倍
 *       （中心不变），逐帧收敛到真实范围，极端情况自然涨回整屏；</li>
 *   <li>区域尺寸按 {@link #STEP} 量化 + 只在"变小"且变化不足一步时沿用上一帧区域，
 *       避免动画在量化边界上抖动导致每帧重建 {@code GpuTexture}；放大总是立即生效。</li>
 * </ol>
 *
 * <p>实测值来自 GPU 提交前的顶点极值，因此不依赖任何"猜测余量"：动画伸出去多少就覆盖多少。</p>
 */
public final class PipPreviewBounds {

    /** 量化步长（GUI 像素）：区域尺寸只会按此步长变化，减少 GpuTexture 重分配。 */
    private static final int STEP = 64;

    /** 实测包围盒（绝对 GUI 像素坐标）。{@code clipped} = 上一帧贴到纹理边界，测量值可能偏小。 */
    public record Rect(float x0, float y0, float x1, float y1, boolean clipped) {
    }

    /** 每个预览 key 上一帧的实测包围盒。 */
    private static final Map<Object, Rect> MEASURED = new ConcurrentHashMap<>();

    /** 每个预览 key 上一帧使用的 region（GUI 像素）。 */
    private static final Map<Object, Rect> REGION = new ConcurrentHashMap<>();

    /**
     * 需要自动收紧 region 的 key（HUD / P 键纸娃娃）。只有这些 key 会采集顶点包围盒，
     * 模型网格里的固定预览（每格独立纹理但区域本就紧贴按钮）不做采集，省掉每帧的顶点扫描。
     */
    private static final java.util.Set<Object> AUTO_FIT_KEYS = ConcurrentHashMap.newKeySet();

    // ---------------- 采集窗口（只在渲染线程使用，无需同步） ----------------
    private static @Nullable Object captureKey;
    private static int captureOriginX;
    private static int captureOriginY;
    private static int captureGuiScale = 1;
    private static int captureTextureWidth;
    private static int captureTextureHeight;
    private static float minX = Float.MAX_VALUE;
    private static float minY = Float.MAX_VALUE;
    private static float maxX = -Float.MAX_VALUE;
    private static float maxY = -Float.MAX_VALUE;
    private static boolean captured;

    private PipPreviewBounds() {
    }

    /**
     * 开始采集一次 PiP 渲染的顶点包围盒。
     *
     * @param key              预览 key（与 {@link YsmPreviewRenderState#key()} 一致）
     * @param originX          区域左上角 x（GUI 像素）
     * @param originY          区域左上角 y（GUI 像素）
     * @param guiScale         本帧 GUI 缩放（纹理像素 / GUI 像素）
     * @param textureWidth     纹理宽（像素）
     * @param textureHeight    纹理高（像素）
     */
    public static void beginCapture(Object key, int originX, int originY, int guiScale,
                                    int textureWidth, int textureHeight) {
        if (!AUTO_FIT_KEYS.contains(key)) {
            captureKey = null;
            return;
        }
        captureKey = key;
        captureOriginX = originX;
        captureOriginY = originY;
        captureGuiScale = Math.max(1, guiScale);
        captureTextureWidth = Math.max(1, textureWidth);
        captureTextureHeight = Math.max(1, textureHeight);
        minX = Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
        maxX = -Float.MAX_VALUE;
        maxY = -Float.MAX_VALUE;
        captured = false;
    }

    public static boolean isCapturing() {
        return captureKey != null;
    }

    /**
     * 采集一批实体格式顶点（布局：每顶点 8 float = {@code px py pz u v nx ny nz}）。
     * 由 {@code GeoBufferSource} 在提交前调用；坐标已在纹理像素空间。
     */
    public static void addVertices(float[] floats, int vertexCount) {
        if (captureKey == null || floats == null) {
            return;
        }
        for (int v = 0; v < vertexCount; v++) {
            int o = v * 8;
            float x = floats[o];
            float y = floats[o + 1];
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
        captured = true;
    }

    /** 结束采集：换算成绝对屏幕坐标，并标记是否贴到纹理边界（= 测量值可能被裁）。 */
    public static void endCapture() {
        Object key = captureKey;
        captureKey = null;
        if (key == null || !captured) {
            return;
        }
        boolean clipped = minX <= 1.0f || minY <= 1.0f
                || maxX >= captureTextureWidth - 1.0f || maxY >= captureTextureHeight - 1.0f;
        float scale = captureGuiScale;
        MEASURED.put(key, new Rect(
                captureOriginX + minX / scale,
                captureOriginY + minY / scale,
                captureOriginX + maxX / scale,
                captureOriginY + maxY / scale,
                clipped));
    }

    /**
     * 规划本帧的 region（GUI 像素，左闭右开，已裁剪到屏幕内）。
     *
     * <p>规则（简洁且自愈，不依赖"上一帧区域"的状态）：</p>
     * <ul>
     *   <li>还没有任何实测数据 → 整屏（第一帧，或模型尚未接管渲染时，保证不裁）；</li>
     *   <li>否则区域 = ({@code 配置框 ∪ 实测包围盒}) 的包围框 + 四周 {@code margin}，
     *       实测贴边（clipped）时该包围框再放大 1.5 倍——贴边说明测量值只是下界，
     *       放大后下一帧能测到更大的范围，逐帧收敛；内容缩回时区域随之缩回。</li>
     * </ul>
     *
     * <p>与"上一帧区域"无关，因此不会出现"缩→贴边→涨→缩"的逐帧来回；尺寸按 {@link #STEP}
     * 量化，动画的亚像素抖动不会导致每帧重建纹理。</p>
     *
     * @param key     预览 key
     * @param frameX0 配置框（绿/蓝方块可调的那个 1×2 参照框）左边界
     * @param frameY0 配置框上边界
     * @param frameX1 配置框右边界
     * @param frameY1 配置框下边界
     * @param margin  内容外围留白（GUI 像素）
     * @return {@code {x0, y0, x1, y1}}
     */
    public static int[] planRegion(Object key, float frameX0, float frameY0, float frameX1, float frameY1,
                                   float margin, int screenWidth, int screenHeight) {
        int[] fullScreen = {0, 0, screenWidth, screenHeight};
        Rect measured = MEASURED.get(key);
        if (measured == null) {
            return fullScreen;
        }

        float x0 = Math.min(frameX0, measured.x0());
        float y0 = Math.min(frameY0, measured.y0());
        float x1 = Math.max(frameX1, measured.x1());
        float y1 = Math.max(frameY1, measured.y1());
        if (measured.clipped()) {
            // 上一帧贴到纹理边界：测量值只是下界，按 1.5 倍放大（中心不变）
            float cx = (x0 + x1) * 0.5f;
            float cy = (y0 + y1) * 0.5f;
            x0 = cx - (cx - x0) * 1.5f;
            x1 = cx + (x1 - cx) * 1.5f;
            y0 = cy - (cy - y0) * 1.5f;
            y1 = cy + (y1 - cy) * 1.5f;
        }

        int rx0 = Math.max(0, Math.min(screenWidth, floorToStep(x0 - margin)));
        int ry0 = Math.max(0, Math.min(screenHeight, floorToStep(y0 - margin)));
        int rx1 = Math.max(0, Math.min(screenWidth, ceilToStep(x1 + margin)));
        int ry1 = Math.max(0, Math.min(screenHeight, ceilToStep(y1 + margin)));
        if (rx1 - rx0 < STEP || ry1 - ry0 < STEP) {
            // 区域过小（异常数据）→ 退回整屏，保证不裁
            return fullScreen;
        }
        // 收缩抖动保护：动画让包围盒在量化边界上抖动时（差 < 1 步且不需要变大），保持上一帧区域，
        // 避免每帧重建 GpuTexture。放大总是立即生效，所以不会因此造成裁剪。
        Rect last = REGION.get(key);
        if (last != null
                && Math.abs(last.x0() - rx0) < STEP && Math.abs(last.y0() - ry0) < STEP
                && Math.abs(last.x1() - rx1) < STEP && Math.abs(last.y1() - ry1) < STEP
                && rx0 >= last.x0() && ry0 >= last.y0() && rx1 <= last.x1() && ry1 <= last.y1()) {
            rx0 = (int) last.x0();
            ry0 = (int) last.y0();
            rx1 = (int) last.x1();
            ry1 = (int) last.y1();
        } else {
            REGION.put(key, new Rect(rx0, ry0, rx1, ry1, false));
        }
        return new int[]{rx0, ry0, rx1, ry1};
    }

    /** 界面关闭 / 预览实体销毁时清理，避免 key 泄漏（纸娃娃的 key 是界面实例或常量，量很小）。 */
    public static void clear(Object key) {
        MEASURED.remove(key);
        REGION.remove(key);
        AUTO_FIT_KEYS.remove(key);
    }

    /** 标记该 key 需要自动收紧 region（由 {@code renderPlayerOverlay} 每帧调用，幂等）。 */
    public static void markAutoFit(Object key) {
        AUTO_FIT_KEYS.add(key);
    }

    private static int floorToStep(float value) {
        return (int) Math.floor(value / STEP) * STEP;
    }

    private static int ceilToStep(float value) {
        return (int) Math.ceil(value / STEP) * STEP;
    }
}
