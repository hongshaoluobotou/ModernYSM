package com.elfmcys.yesstevemodel.client.renderer.layer;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.renderer.GeoBufferSource;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLayerRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import com.mojang.math.Axis;

// 26.3 port: ParrotModel 绑定 ParrotRenderState（旧 renderOnShoulder 已移除），
// 通过 setupAnim(ParrotRenderState) + renderToBuffer 提交。
public class CustomPlayerParrotLayer extends GeoLayerRenderer<CustomPlayerEntity> {

    private static final String TAG_ID = "id";

    private static final String TAG_VARIANT = "Variant";

    private final ParrotModel parrotModel;

    public CustomPlayerParrotLayer(EntityRendererProvider.Context context) {
        this.parrotModel = new ParrotModel(context.bakeLayer(ModelLayers.PARROT));
    }

    @Override
    public void render(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, GeoBufferSource bufferSource, int packedLightIn, CustomPlayerEntity entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, AvatarRenderState renderState) {
        Player player = entityLivingBaseIn.getEntity();
        AnimatedGeoModel model = entityLivingBaseIn.getCurrentModel();
        if (model == null) {
            return;
        }
        if (!model.leftShoulderBones().isEmpty()) {
            renderParrot(poseStack, bufferSource, model, packedLightIn, player, limbSwing, limbSwingAmount, netHeadYaw, headPitch, true);
        }
        if (!model.rightShoulderBones().isEmpty()) {
            renderParrot(poseStack, bufferSource, model, packedLightIn, player, limbSwing, limbSwingAmount, netHeadYaw, headPitch, false);
        }
    }

    private void renderParrot(PoseStack poseStack, GeoBufferSource bufferSource, AnimatedGeoModel model, int packedLightIn, Player player, float limbSwing, float limbSwingAmount, float netHeadYaw, float headPitch, boolean isLeftShoulder) {
        // 26.3 port: 肩膀实体改为强类型 Optional<Parrot.Variant>（getShoulderParrotLeft/Right），
        // 不再读 NBT tag / EntityType.byString。
        Optional<Parrot.Variant> shoulderParrot = isLeftShoulder ? player.getShoulderParrotLeft() : player.getShoulderParrotRight();
        shoulderParrot.ifPresent(variant -> {
            poseStack.pushPose();
            applyParrotTransform(poseStack, model, isLeftShoulder);
            poseStack.translate(0.0d, 1.5d, 0.0d);
            poseStack.rotate(Axis.ZP.rotationDegrees(180.0f));
            ParrotRenderState parrotState = new ParrotRenderState();
            parrotState.variant = variant;
            parrotState.pose = ParrotModel.Pose.ON_SHOULDER;
            this.parrotModel.setupAnim(parrotState);
            this.parrotModel.renderToBuffer(poseStack, bufferSource.getBuffer(this.parrotModel.renderType(ParrotRenderer.getVariantTexture(parrotState.variant))), packedLightIn, 0, -1);
            poseStack.popPose();
        });
    }

    public void applyParrotTransform(PoseStack poseStack, AnimatedGeoModel model, boolean isLeftShoulder) {
        if (isLeftShoulder) {
            RenderUtils.prepMatrixForLocator(poseStack, model.leftShoulderBones());
        } else {
            RenderUtils.prepMatrixForLocator(poseStack, model.rightShoulderBones());
        }
    }
}
