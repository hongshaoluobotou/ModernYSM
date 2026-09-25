package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.client.renderer.GeoBufferSource;
import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

// 26.3 port: MultiBufferSource → SubmitNodeCollector + GeoBufferSource（submit 渲染体系）
public abstract class GeoLayerRenderer<T extends AnimatableEntity<?>> {
    public abstract void render(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, GeoBufferSource bufferSource, int packedLightIn, T entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, AvatarRenderState renderState);
}
