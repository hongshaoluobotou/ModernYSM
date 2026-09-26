package com.elfmcys.yesstevemodel.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 26.3 port: 暴露 {@link EntityRenderer#submitNameDisplay}（protected）。
 * YSM 接管玩家渲染时跳过 vanilla 的 {@code EntityRenderer#submit}（避免原版模型叠画），
 * 但仍需手动提交名牌/缰绳——名牌经此 Invoker 虚分派到子类重载
 * （如 {@code AvatarRenderer#submitNameDisplay(AvatarRenderState,...)}，含 deadmau5 耳朵偏移），与原版行为一致。
 */
@Mixin(EntityRenderer.class)
public interface EntityRendererInvoker {

    @Invoker("submitNameDisplay")
    <S extends EntityRenderState> void ysm$submitNameDisplay(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera);
}
