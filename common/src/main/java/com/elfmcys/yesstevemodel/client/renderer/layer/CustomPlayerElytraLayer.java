package com.elfmcys.yesstevemodel.client.renderer.layer;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import rip.ysm.compat.cosmeticarmorreworked.CosmeticArmorHelper;
import com.elfmcys.yesstevemodel.client.renderer.GeoBufferSource;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLayerRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import com.mojang.math.Axis;

// 26.3 port: ElytraModel 移至 net.minecraft.client.model.object.equipment 且绑定 HumanoidRenderState；
// 渲染通过 GeoBufferSource.getBuffer(RenderTypes.armorCutoutNoCull(...)) + renderToBuffer。
public class CustomPlayerElytraLayer extends GeoLayerRenderer<CustomPlayerEntity> {

    private static final Identifier WINGS_LOCATION = Identifier.parse("textures/entity/elytra.png");

    private final ElytraModel elytraModel;

    public CustomPlayerElytraLayer(EntityRendererProvider.Context context) {
        this.elytraModel = new ElytraModel(context.bakeLayer(ModelLayers.ELYTRA));
    }

    @Override
    public void render(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, GeoBufferSource bufferSource, int packedLightIn, CustomPlayerEntity entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, AvatarRenderState renderState) {
        Identifier cloakTextureLocation;
        LivingEntity entity = entityLivingBaseIn.getEntity();
        ItemStack stack = CosmeticArmorHelper.getElytraItem(entity);
        AnimatedGeoModel animatedGeoModel = entityLivingBaseIn.getCurrentModel();
        if (!stack.isEmpty() && animatedGeoModel != null && !animatedGeoModel.elytraBones().isEmpty() && (entity instanceof AbstractClientPlayer abstractClientPlayer)) {
            // 26.3 port: AbstractClientPlayer 皮肤改为 PlayerSkin record（getSkin()），
            // isElytraLoaded/getElytraTextureLocation/isCapeLoaded/getCloakTextureLocation 已删除；
            // 皮肤由 PlayerSkinRenderCache 异步加载，此处直接取 texturePath。
            PlayerSkin skin = abstractClientPlayer.getSkin();
            if (skin.elytra() != null) {
                cloakTextureLocation = skin.elytra().texturePath();
            } else if (skin.cape() != null && abstractClientPlayer.isModelPartShown(PlayerModelPart.CAPE)) {
                cloakTextureLocation = skin.cape().texturePath();
            } else {
                cloakTextureLocation = WINGS_LOCATION;
            }
            poseStack.pushPose();
            renderElytra(poseStack, animatedGeoModel);
            poseStack.translate(0.0d, 1.5d, 0.0d);
            poseStack.rotate(Axis.ZP.rotationDegrees(180.0f));
            poseStack.scale(2.0f, 2.0f, 2.0f);
            this.elytraModel.setupAnim(renderState);
            this.elytraModel.renderToBuffer(poseStack, bufferSource.getBuffer(RenderTypes.armorCutoutNoCull(cloakTextureLocation)), packedLightIn, 0, -1);
            poseStack.popPose();
        }
    }

    public void renderElytra(PoseStack poseStack, AnimatedGeoModel model) {
        RenderUtils.prepMatrixForLocator(poseStack, model.elytraBones());
    }
}
