package com.elfmcys.yesstevemodel.neoforge.mixin.client;

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
 * 26.3 背包预览接管（NeoForge 版）。
 *
 * <p>与 fabric 版（{@code fabric.mixin.client.InventoryScreenMixin}）是同一逻辑的两份，唯一差异是
 * {@code @Inject} 的目标方法：<b>NeoForge 把 {@code extractEntityInInventoryFollowsMouse} 的方法体
 * 搬到了它新增的 {@code renderEntityInInventoryFollowsAngle}（前者只剩一句委托）</b>，
 * 若仍注入 follows-mouse 会在加载期抛 "Critical injection failure ... Scanned 0 target(s)"。
 * 注入 follows-angle 后，所有复用 follows-mouse 的入口（{@code CreativeModeInventoryScreen}、
 * {@code AbstractMountInventoryScreen} 等）经委托同样被覆盖。</p>
 *
 * <p>公共部分（在两个加载器上完全一致）：{@code @WrapOperation} 把私有静态
 * {@code extractRenderState(LivingEntity)} 里的 {@code renderer.createRenderState(entity, 1.0f)}
 * 换成 {@code dispatcher.extractEntity(entity, partialTick)}，以填充 state→entity 弱键表、
 * 让 YSM 接管在 PiP 预览里生效。</p>
 */
@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @WrapOperation(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"))
    private static EntityRenderState ysm$extractViaDispatcher(EntityRenderer<?, ?> renderer, Entity entity, float partialTick, Operation<EntityRenderState> original) {
        return Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(entity, partialTick);
    }

    @Inject(method = "renderEntityInInventoryFollowsAngle", at = @At("RETURN"))
    private static void ysm$captureInventoryPreviewRotation(GuiGraphicsExtractor guiGraphics, int x0, int y0, int x1, int y1, int scale, float angleX, float angleY, float unused, LivingEntity entity, CallbackInfo ci, @Local(ordinal = 0) EntityRenderState state) {
        if (state instanceof LivingEntityRenderState living) {
            ModelPreviewRenderer.INVENTORY_PREVIEW_ROT.put(state, new float[]{living.bodyRot, living.xRot, living.yRot, 1.0f});
        }
    }
}
