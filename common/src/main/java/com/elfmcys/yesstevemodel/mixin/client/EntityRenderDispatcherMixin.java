package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.ReplacePlayerRenderEvent;
import com.elfmcys.yesstevemodel.client.renderer.CustomFishingHookRenderer;
import com.elfmcys.yesstevemodel.client.renderer.CustomProjectileRenderer;
import com.elfmcys.yesstevemodel.client.renderer.CustomVehicleRenderer;
import com.elfmcys.yesstevemodel.client.renderer.GeoBufferSource;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * 26.3 port: 1.20.1 的渲染接管点（EntityRenderer.render / MultiBufferSource）已不存在，
 * 改为在 {@link EntityRenderDispatcher#submit} 内部包裹 vanilla 的
 * {@code renderer.submit(state, poseStack, collector, camera)} 调用：
 * <ul>
 *   <li>玩家：命中 YSM 模型时改走 {@code CustomPlayerRenderer#renderPlayer}（submit 体系）；</li>
 *   <li>弹射物/鱼钩/载具：命中时经 {@link GeoBufferSource} 延迟提交自定义 Geo 模型；</li>
 *   <li>接管时跳过 vanilla {@code EntityRenderer#submit}（1.20.1 语义：取消原版模型渲染，避免叠画），
 *       但手动补提交名牌（{@link EntityRendererInvoker}，虚分派到 AvatarRenderer 重载）与缰绳，
 *       与原版非模型状态提交一致。</li>
 * </ul>
 * 实体与渲染状态的关联：{@code extractEntity} 返回时记录 (state → entity, partialTick)（弱键表）。
 * TODO port: 26.3 玩家渲染状态可能不经 EntityRenderDispatcher#extractEntity 提取（PlayerSkinRenderCache 路径），需 runClient 验证。
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Unique
    private static final Map<EntityRenderState, Entity> ysm$stateToEntity = new com.google.common.collect.MapMaker()
            .weakKeys()
            .weakValues()
            .makeMap();

    @Unique
    private static final Map<EntityRenderState, Float> ysm$stateToPartialTick = new com.google.common.collect.MapMaker()
            .weakKeys()
            .weakValues()
            .makeMap();

    @Inject(method = "extractEntity", at = @At("RETURN"))
    private <E extends Entity> void ysm$captureStateEntity(E entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
        EntityRenderState state = cir.getReturnValue();
        if (entity != null && state != null) {
            ysm$stateToEntity.put(state, entity);
            ysm$stateToPartialTick.put(state, partialTick);
        }
    }

    @SuppressWarnings("unchecked")
    @WrapOperation(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"))
    private <S extends EntityRenderState> void ysm$wrapSubmit(EntityRenderer<?, ? super S> renderer, S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, Operation<Void> original) {
        Entity entity = ysm$stateToEntity.get(state);
        // true = 自定义模型已渲染，需要跳过 vanilla 渲染（1.20.1 语义）。
        // 注意：CustomVehicleRenderer/CustomProjectileRenderer/CustomFishingHookRenderer 的返回值
        // 沿用 1.20.1 WrapWithCondition 的语义——true = 保留 vanilla 渲染（实体不可接管），
        // false = 已渲染自定义模型、应取消 vanilla。与此处 handled 相反，故取反（曾因未取反导致全部实体不可见）。
        boolean handled = false;
        if (entity != null && YesSteveModel.isAvailable()) {
            float partialTick = ysm$stateToPartialTick.getOrDefault(state, 0.0f);
            EntityRenderDispatcher dispatcher = (EntityRenderDispatcher) (Object) this;
            int packedLight = dispatcher.getPackedLightCoords(entity, partialTick);
            if (entity instanceof Player player) {
                if (state instanceof AvatarRenderState avatarState) {
                    // onRenderPlayerPre 的返回值是"已取消"语义（true = 跳过 vanilla），与 handled 一致。
                    handled = ReplacePlayerRenderEvent.onRenderPlayerPre(player, partialTick, poseStack, submitNodeCollector, camera, avatarState);
                }
            } else if (entity instanceof Projectile projectile) {
                if (!GeneralConfig.DISABLE_PROJECTILE_MODEL.get()) {
                    GeoBufferSource bufferSource = new GeoBufferSource();
                    boolean keepVanilla;
                    if (projectile instanceof FishingHook fishingHook) {
                        keepVanilla = CustomFishingHookRenderer.tryRenderCustomHook(fishingHook, entity.getYRot(), partialTick, poseStack, bufferSource, packedLight);
                    } else {
                        keepVanilla = CustomProjectileRenderer.renderProjectile(projectile, entity.getYRot(), partialTick, poseStack, bufferSource, packedLight);
                    }
                    handled = !keepVanilla;
                    if (handled) {
                        bufferSource.flush(submitNodeCollector, poseStack);
                    }
                }
            } else if (!GeneralConfig.DISABLE_VEHICLE_MODEL.get()) {
                CustomVehicleRenderer.applyPassengerPose(entity, poseStack, partialTick);
                GeoBufferSource bufferSource = new GeoBufferSource();
                boolean keepVanilla = CustomVehicleRenderer.renderVehicle(entity, entity.getYRot(), partialTick, poseStack, bufferSource, packedLight);
                handled = !keepVanilla;
                if (handled) {
                    bufferSource.flush(submitNodeCollector, poseStack);
                }
            }
        }
        if (handled) {
            // 1.20.1 语义：接管 = 取消原版渲染。跳过 original（含原版模型/layers），
            // 仅补提交 vanilla EntityRenderer#submit 中的非模型部分（缰绳 + 名牌）。
            if (state.leashStates != null) {
                for (EntityRenderState.LeashState leashState : state.leashStates) {
                    submitNodeCollector.submitLeash(poseStack, leashState);
                }
            }
            ((EntityRendererInvoker) renderer).ysm$submitNameDisplay(state, poseStack, submitNodeCollector, camera);
            return;
        }
        // 未接管：完整走 vanilla EntityRenderer#submit（模型 + 名牌/缰绳等状态提交）
        original.call(renderer, state, poseStack, submitNodeCollector, camera);
    }
}
