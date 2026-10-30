package com.elfmcys.yesstevemodel.client.renderer.pip;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

/**
 * YSM 实体预览 PiP 渲染器池（按 {@link YsmPreviewRenderState#key()} 一 key 一实例）。
 *
 * <p>26.3 的 {@code GuiRenderer} 对每个 PiP 状态类只持有一个 renderer（一份纹理），
 * 同帧多个同类状态会互相覆盖（根因见 {@link YsmPreviewRenderState}）。这里照抄 vanilla
 * {@code OversizedItemRenderer} 的做法：renderer 按 key 池化、帧末回收未用到的实例。</p>
 *
 * <p>调用链：{@code GuiRendererPreviewMixin} 在 {@code GuiRenderer.preparePictureInPictureState}
 * HEAD 拦截 {@link YsmPreviewRenderState} → {@link #prepare}；在 {@code GuiRenderer.render}
 * RETURN 调用 {@link #endFrame} 回收。</p>
 */
public final class YsmPreviewRendererPool {

    private static final Map<Object, YsmPreviewRenderer> RENDERERS = new LinkedHashMap<>();

    private YsmPreviewRendererPool() {
    }

    public static void prepare(YsmPreviewRenderState renderState, GuiRenderState guiRenderState,
                               FeatureRenderDispatcher featureRenderDispatcher, int guiScale) {
        YsmPreviewRenderer renderer = RENDERERS.get(renderState.key());
        if (renderer == null) {
            renderer = new YsmPreviewRenderer(Minecraft.getInstance().getEntityRenderDispatcher());
            RENDERERS.put(renderState.key(), renderer);
        }
        renderer.prepare(renderState, guiRenderState, featureRenderDispatcher, guiScale);
    }

    /**
     * 帧末回收：本帧没有 blit 的预览（界面关闭、滚出可视区、模型列表换页等）连同纹理一起释放，
     * 其余重置使用标记。与 vanilla {@code GuiRenderer#clearUnusedOversizedItemRenderers} 同构。
     */
    public static void endFrame() {
        Iterator<Map.Entry<Object, YsmPreviewRenderer>> iterator = RENDERERS.entrySet().iterator();
        while (iterator.hasNext()) {
            YsmPreviewRenderer renderer = iterator.next().getValue();
            if (!renderer.usedOnThisFrame()) {
                renderer.close();
                iterator.remove();
            } else {
                renderer.resetUsedOnThisFrame();
            }
        }
    }
}
