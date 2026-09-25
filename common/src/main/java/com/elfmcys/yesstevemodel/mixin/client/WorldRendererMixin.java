package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.entity.EntityRenderCache;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 26.3 port: 1.20.1 的 renderLevel(PoseStack, float, ...) 已删除，改为
// render(GraphicsResourceAllocator, boolean, CameraRenderState, GpuBufferSlice, Vector4f, boolean, boolean)。
// 动画驱动的语义保持不变：
//  - 帧开始：setFirstPersonMode(true) + EntityRenderCache.tick(partialTick)
//    （后台线程预计算动画控制器时间线 —— 渲染时 processAnimation 走 isFirstPerson 路径消费该结果，
//     否则 controller.process 永不执行，模型静止在初始姿势）；
//  - 帧结束：EntityRenderCache.clear()（等待/回收异步结果）+ setFirstPersonMode(false)。
@Mixin({LevelRenderer.class})
public class WorldRendererMixin {
    @Inject(method = {"render"}, at = {@At("HEAD")})
    private void renderLevel(CallbackInfo ci) {
        if (YesSteveModel.isAvailable() && Minecraft.getInstance().level != null) {
            ModelPreviewRenderer.setFirstPersonMode(true);
            EntityRenderCache.tick(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
        }
    }

    @Inject(method = {"render"}, at = {@At("RETURN")})
    private void renderLevelPost(CallbackInfo ci) {
        if (YesSteveModel.isAvailable()) {
            EntityRenderCache.clear();
            ModelPreviewRenderer.setFirstPersonMode(false);
        }
    }
}
