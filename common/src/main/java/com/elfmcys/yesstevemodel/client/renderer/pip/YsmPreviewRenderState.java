package com.elfmcys.yesstevemodel.client.renderer.pip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * YSM 自定义 PiP（Pictures-in-Picture）实体预览状态。
 *
 * <p><b>为什么不直接用 vanilla {@code GuiEntityRenderState}（2026.11.02 修复）</b>：
 * 26.3 的 {@code GuiRenderer} 用 {@code Map<Class<? extends PictureInPictureRenderState>,
 * PictureInPictureRenderer<?>> pictureInPictureRenderers} 分发 PiP——<b>每个状态类只对应一个
 * renderer 实例</b>，而 {@code PictureInPictureRenderer} 只持有一份 {@code texture/textureView}。
 * {@code prepare()} 会把同类的所有状态逐个渲染进<b>同一张纹理</b>，并把 blit 记进
 * GuiRenderState；真正的绘制发生在全部 prepare 之后，于是所有 blit 采样到的都是
 * <b>最后一个渲染的状态</b>——表现为"前一个预览被后一个覆盖"（模型按钮网格全部显示同一个模型，
 * 左侧大预览也被覆盖）。vanilla 只有背包里的单个实体预览，故不受影响。</p>
 *
 * <p>解决办法（照抄 vanilla {@code OversizedItemRenderer} 的先例）：自定义状态类 +
 * 按 {@link #key()} 池化 renderer 实例（每个 key 一份纹理），见
 * {@link YsmPreviewRendererPool}。{@code key} 取"预览用途"的稳定标识：
 * 网格里的每个预览实体对象、主预览/纸娃娃的常量或界面实例。</p>
 *
 * <p>字段语义与 {@code GuiEntityRenderState} 逐项对应（x0/y0/x1/y1 = 区域、scale = GUI 像素/块）；
 * {@code decor} 是 YSM 独有的预览装饰（床 / 草地 / 载具，见 {@link YsmPreviewDecor}），
 * 只有动画测试界面会用到。</p>
 */
public record YsmPreviewRenderState(
        EntityRenderState renderState,
        Vector3fc translation,
        Quaternionfc rotation,
        @Nullable Quaternionfc overrideCameraAngle,
        int x0,
        int y0,
        int x1,
        int y1,
        float scale,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds,
        @Nullable YsmPreviewDecor decor,
        Object key
) implements PictureInPictureRenderState {

    public YsmPreviewRenderState(EntityRenderState renderState,
                                 Vector3fc translation,
                                 Quaternionfc rotation,
                                 @Nullable Quaternionfc overrideCameraAngle,
                                 int x0, int y0, int x1, int y1,
                                 float scale,
                                 @Nullable ScreenRectangle scissorArea,
                                 @Nullable YsmPreviewDecor decor,
                                 Object key) {
        this(renderState, translation, rotation, overrideCameraAngle, x0, y0, x1, y1, scale, scissorArea,
                PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea), decor, key);
    }
}
