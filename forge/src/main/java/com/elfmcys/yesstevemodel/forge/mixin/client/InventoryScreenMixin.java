package com.elfmcys.yesstevemodel.forge.mixin.client;

import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.3 背包预览接管（Forge 版）。
 *
 * <p>与 fabric 版（{@code fabric.mixin.client.InventoryScreenMixin}）逐行相同：Forge 26.3 的 patch
 * 只动了 {@code CreativeModeInventoryScreen}，没有改 {@code InventoryScreen} 的
 * {@code extractEntityInInventoryFollowsMouse}（NeoForge 才把它搬到了
 * {@code renderEntityInInventoryFollowsAngle}，故 NeoForge 版注入目标不同）。</p>
 *
 * <p>公共部分：{@code @WrapOperation} 把私有静态 {@code extractRenderState(LivingEntity)} 里的
 * {@code renderer.createRenderState(entity, 1.0f)} 换成
 * {@code dispatcher.extractEntity(entity, partialTick)}，以填充 state→entity 弱键表、
 * 让 YSM 接管在 PiP 预览里生效；RETURN 处按渲染状态对象弱键登记鼠标跟随旋转。</p>
 */
@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @WrapOperation(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"))
    private static EntityRenderState ysm$extractViaDispatcher(EntityRenderer<?, ?> renderer, Entity entity, float partialTick, Operation<EntityRenderState> original) {
        return Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(entity, partialTick);
    }

    @Inject(method = "extractEntityInInventoryFollowsMouse", at = @At("RETURN"))
    private static void ysm$captureInventoryPreviewRotation(GuiGraphicsExtractor guiGraphics, int x0, int y0, int x1, int y1, int scale, float f0, float mouseX, float mouseY, LivingEntity entity, CallbackInfo ci, @Local(ordinal = 0) EntityRenderState state) {
        if (state instanceof LivingEntityRenderState living) {
            ModelPreviewRenderer.INVENTORY_PREVIEW_ROT.put(state, new float[]{living.bodyRot, living.xRot, living.yRot, 1.0f});
        }
    }
}
