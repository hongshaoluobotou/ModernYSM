package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.3 port: 1.20.1 的 {@code InventoryScreenMixin}（HEAD/RETURN 包
 * {@code renderEntityInInventoryFollowsMouse} 置 ModelPreviewRenderer 预览标志）的等价移植。
 *
 * <p>1.20.1 中该 vanilla 方法直接改写预览实体（背包即本地玩家）的 yBodyRot/yRot/xRot，
 * YSM 的接管走 EntityRenderDispatcher 渲染路径自然生效，mixin 只需置预览标志。
 * 26.3 的 {@code InventoryScreen.extractEntityInInventoryFollowsMouse} 有两处不同：</p>
 * <ol>
 *   <li>内部 {@code extractRenderState} 直接调 {@code renderer.createRenderState(entity, 1.0f)}，
 *       绕过 {@link EntityRenderDispatcher#extractEntity} —— EntityRenderDispatcherMixin 的
 *       state→entity 弱键表不被填充，PiP 渲染时 dispatcher.submit 的 YSM 接管不生效
 *       （画原版皮肤，与 GUI 自定义预览重写时发现的同一个坑）。这里 @WrapOperation 将该
 *       createRenderState 调用替换为 {@code dispatcher.extractEntity(entity, partialTick)}
 *       （后者即 getRenderer + createRenderState + crash 包装，语义等价且自动填表）。</li>
 *   <li>鼠标跟随旋转改设在 {@link LivingEntityRenderState} 的 bodyRot/yRot/xRot 字段上，
 *       YSM geo 渲染路径读实体字段（见 ModelPreviewRenderer.PREVIEW_YAW 注释）。方法 RETURN 时
 *       以渲染状态对象为键把旋转暂存 {@link ModelPreviewRenderer#INVENTORY_PREVIEW_ROT}，
 *       由 CustomPlayerRenderer#renderPlayer 在 submit 时改写/还原实体旋转。</li>
 * </ol>
 *
 * <p>该 mixin 同时覆盖 {@code CreativeModeInventoryScreen} / {@code AbstractMountInventoryScreen}
 * 等复用同一 static 方法的入口（其预览实体非玩家时 YSM 不接管，仅多一条随 state 回收的弱键）。</p>
 */
@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @WrapOperation(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"))
    private static EntityRenderState ysm$extractViaDispatcher(EntityRenderer<?, ?> renderer, net.minecraft.world.entity.Entity entity, float partialTick, Operation<EntityRenderState> original) {
        return net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(entity, partialTick);
    }

    @Inject(method = "extractEntityInInventoryFollowsMouse", at = @At("RETURN"))
    private static void ysm$captureInventoryPreviewRotation(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, int x0, int y0, int x1, int y1, int scale, float f0, float mouseX, float mouseY, LivingEntity entity, CallbackInfo ci, @Local(ordinal = 0) EntityRenderState state) {
        if (state instanceof LivingEntityRenderState living) {
            ModelPreviewRenderer.INVENTORY_PREVIEW_ROT.put(state, new float[]{living.bodyRot, living.yRot, living.xRot});
        }
    }
}
