package com.elfmcys.yesstevemodel.fabric.mixin.client;

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
 * 26.3 背包预览接管（Fabric 版）。
 *
 * <p>NeoForge 会把 {@code extractEntityInInventoryFollowsMouse} 的方法体搬到它新增的
 * {@code renderEntityInInventoryFollowsAngle} 里（前者只剩一句委托），因此两个加载器的
 * mixin 目标方法不同，本类与 {@code neoforge.mixin.client.InventoryScreenMixin} 是同一逻辑的两份：
 * 公共部分是 {@code @WrapOperation} 到私有静态 {@code extractRenderState(LivingEntity)}，
 * 差异部分只有 {@code @Inject} 的目标方法名。</p>
 *
 * <p>1.20.1 中该 vanilla 方法直接改写预览实体（背包即本地玩家）的 yBodyRot/yRot/xRot，
 * YSM 的接管走 EntityRenderDispatcher 渲染路径自然生效，mixin 只需置预览标志。
 * 26.3 的 {@code extractEntityInInventoryFollowsMouse} 有两处不同：</p>
 * <ol>
 *   <li>内部 {@code extractRenderState} 直接调 {@code renderer.createRenderState(entity, 1.0f)}，
 *       绕过 {@code EntityRenderDispatcher#extractEntity} —— EntityRenderDispatcherMixin 的
 *       state→entity 弱键表不被填充，PiP 渲染时 dispatcher.submit 的 YSM 接管不生效。
 *       这里 @WrapOperation 将该 createRenderState 调用替换为
 *       {@code dispatcher.extractEntity(entity, partialTick)}（语义等价且自动填表）。</li>
 *   <li>鼠标跟随旋转改设在 {@link LivingEntityRenderState} 的 bodyRot/yRot/xRot 字段上，
 *       YSM geo 渲染路径读实体字段（见 ModelPreviewRenderer.PREVIEW_YAW 注释）。方法 RETURN 时
 *       以渲染状态对象为键把旋转暂存 {@link ModelPreviewRenderer#INVENTORY_PREVIEW_ROT}，
 *       由 CustomPlayerRenderer#renderPlayer 在 submit 时改写/还原实体旋转。</li>
 * </ol>
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
