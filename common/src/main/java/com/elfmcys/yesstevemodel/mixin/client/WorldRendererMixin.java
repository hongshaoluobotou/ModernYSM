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
// 动画驱动的语义对齐 1.20.1（两段式时序）：
//  - 帧开始：setFirstPersonMode(true) + EntityRenderCache.tick(partialTick)
//    （后台线程预计算动画控制器时间线 —— processAnimation 的 z3=true 路径才会推进 controller）；
//  - 立即 clear() 等待异步结果 + setFirstPersonMode(false)。
//    ⚠️ 1.20.1 的 setFirstPersonMode(false) 注入在 renderLevel 内第一次 RenderType.entitySolid
//    调用处（实体渲染开场），即 YSM 消费求值结果时 firstPerson 标志**必须已为 false**——
//    若保持 true 到实体渲染结束，molang 第一人称条件查询（如模型包 Root 骨骼 scale 的
//    `query.is_first_person ? 0 : 1` 藏身体写法）会在第三人称下误判，整棵骨骼树被隐藏。
@Mixin({LevelRenderer.class})
public class WorldRendererMixin {
    @Inject(method = {"render"}, at = {@At("HEAD")})
    private void renderLevel(CallbackInfo ci) {
        if (YesSteveModel.isAvailable() && Minecraft.getInstance().level != null) {
            ModelPreviewRenderer.setFirstPersonMode(true);
            EntityRenderCache.tick(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
            EntityRenderCache.clear();
            ModelPreviewRenderer.setFirstPersonMode(false);
        }
    }
}
