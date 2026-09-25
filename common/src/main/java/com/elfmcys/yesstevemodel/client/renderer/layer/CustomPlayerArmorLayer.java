package com.elfmcys.yesstevemodel.client.renderer.layer;

import com.elfmcys.yesstevemodel.client.renderer.GeoBufferSource;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import rip.ysm.compat.simplehats.SimpleHatsHelper;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLayerRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

// 26.3 port: 头盔（非盔甲槽物品）/ 帽子挂接到头部骨骼；
// 物品渲染由 ItemInHandRenderer.renderItem(...) 改为 ItemModelResolver + ItemStackRenderState.submit。
public class CustomPlayerArmorLayer extends GeoLayerRenderer<CustomPlayerEntity> {

    private final ItemModelResolver itemModelResolver;

    public CustomPlayerArmorLayer(EntityRendererProvider.Context context) {
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public void render(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, GeoBufferSource bufferSource, int packedLightIn, CustomPlayerEntity entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, AvatarRenderState renderState) {
        Player player = entityLivingBaseIn.getEntity();
        AnimatedGeoModel model = entityLivingBaseIn.getCurrentModel();
        if (model != null && !model.headBones().isEmpty()) {
            ItemStack itemBySlot = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!itemBySlot.isEmpty() && !isArmorItem(itemBySlot)) {
                renderArmorPiece(poseStack, submitNodeCollector, packedLightIn, model, player, itemBySlot);
            }
            ItemStack stack = SimpleHatsHelper.getHatItem(player);
            if (stack != null && !stack.isEmpty()) {
                renderArmorPiece(poseStack, submitNodeCollector, packedLightIn, model, player, stack);
            }
        }
    }

    private boolean isArmorItem(ItemStack stack) {
        Item item = stack.getItem();
        // 26.3 port: ArmorItem 已删除，头盔判定改用 Equippable 数据组件。
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.HEAD;
    }

    private void renderArmorPiece(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, AnimatedGeoModel model, Player player, ItemStack stack) {
        poseStack.pushPose();
        RenderUtils.prepMatrixForLocator(poseStack, model.headBones());
        poseStack.scale(0.625f, 0.625f, 0.625f);
        poseStack.translate(0.0f, 0.25f, 0.0f);
        ItemStackRenderState itemRenderState = new ItemStackRenderState();
        this.itemModelResolver.updateForLiving(itemRenderState, stack, ItemDisplayContext.HEAD, player);
        if (!itemRenderState.isEmpty()) {
            itemRenderState.submit(poseStack, submitNodeCollector, i, OverlayTexture.NO_OVERLAY, 0);
        }
        poseStack.popPose();
    }
}
