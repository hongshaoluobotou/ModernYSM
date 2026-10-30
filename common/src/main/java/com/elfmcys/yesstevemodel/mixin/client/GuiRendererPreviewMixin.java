package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.client.renderer.pip.YsmPreviewRendererPool;
import com.elfmcys.yesstevemodel.client.renderer.pip.YsmPreviewRenderState;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PiP 预览隔离（2026.11.02）——<b>仅 fabric / forge</b> 注册
 * （配置见 {@code yes_steve_model_pip.mixins.json}）。
 *
 * <p>vanilla {@code GuiRenderer.preparePictureInPictureState} 按状态类从
 * {@code pictureInPictureRenderers} 取<b>唯一</b> renderer，同类状态共用一份纹理 →
 * 同帧多个 YSM 模型预览互相覆盖（"前一个被后一个覆盖"）。这里把
 * {@link YsmPreviewRenderState} 拦下来，交给 {@link YsmPreviewRendererPool}
 * 按 key 池化的独立 renderer（独立纹理）渲染；帧末清理不再使用的 renderer。</p>
 *
 * <p><b>NeoForge 不注册本 mixin</b>：NeoForge patch 已把 {@code GuiRenderer} 改成按状态池化
 * （{@code PictureInPictureRendererPool}，且 target 方法签名也变成
 * {@code boolean preparePictureInPictureState(T,int,boolean)}），自定义状态类改为在
 * {@code RegisterPictureInPictureRenderersEvent}（mod 总线）里注册，池化由 NeoForge 负责。
 * 因此本类只出现在 fabric.mod.json 的 mixins 与 Forge 的 {@code MixinConfigs} 里。</p>
 */
@Mixin(GuiRenderer.class)
public abstract class GuiRendererPreviewMixin {

    @Shadow
    @Final
    private GuiRenderState renderState;

    @Shadow
    @Final
    private FeatureRenderDispatcher featureRenderDispatcher;

    @Inject(
            method = "preparePictureInPictureState(Lnet/minecraft/client/renderer/state/gui/pip/PictureInPictureRenderState;I)V",
            at = @At("HEAD"),
            cancellable = true)
    private void ysm$prepareYsmPreview(PictureInPictureRenderState state, int guiScale, CallbackInfo ci) {
        if (state instanceof YsmPreviewRenderState preview) {
            YsmPreviewRendererPool.prepare(preview, this.renderState, this.featureRenderDispatcher, guiScale);
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void ysm$endPreviewFrame(CallbackInfo ci) {
        YsmPreviewRendererPool.endFrame();
    }
}
