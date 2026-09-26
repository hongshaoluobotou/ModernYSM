package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.client.event.ReplacePlayerHandRenderEvent;
import com.elfmcys.yesstevemodel.client.event.RenderFirstPlayerBackground;
import com.elfmcys.yesstevemodel.client.renderer.GeoBufferSource;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.3 port: 1.20.1 的第一人称手臂接管点（fabric {@code PlayerRendererHandMixin}，HEAD 注入
 * {@code PlayerRenderer#renderRightHand/renderLeftHand}）在 26.3 已不存在——第一人称手部渲染
 * 整体迁入 {@link FirstPersonHandsAndItemsRenderer}（extract/submit 体系）：
 * <ul>
 *   <li>入口 {@code GameRenderer#renderItemInHand}（在 {@code LevelRenderer#render} 返回之后的
 *       {@code render3dHud} 阶段执行，注意此时 {@code RenderBridge.firstPersonOnRenderThread}
 *       已被 {@code WorldRendererMixin} RETURN 清除，本 mixin 需临时恢复 firstPerson 标志，
 *       使 {@code HandItemRenderer} 的 {@code processAnimation} 走与 1.20.1 相同的
 *       firstPerson 动画路径）；</li>
 *   <li>手持物品的手臂：{@code submitArmWithItem} → {@code renderPlayerHand} →
 *       {@code AvatarRenderer#renderRightHand/renderLeftHand}（旧 mixin 的目标方法，
 *       26.3 签名变为 (PoseStack, SubmitNodeCollector, int, Identifier, boolean)，无实体参数），
 *       对应 {@link ReplacePlayerHandRenderEvent}（模型自带 ysm 左右手模型，
 *       {@code hasCustomLeftHand/RightHand}）；</li>
 *   <li>空手/背景肢体：{@code renderPlayerArm}（原版直接画 HumanoidModel 手臂），
 *       与 1.20.1 一致不取消——由 {@link RenderFirstPlayerBackground}（{@code hasCustomLimbs}
 *       时整模 background 提交，遮盖原版手臂）承担。</li>
 * </ul>
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class FirstPersonHandsAndItemsRendererMixin {

    /**
     * 对应 1.20.1 fabric {@code PlayerRendererHandMixin}：手持物品手臂（原版皮肤手）接管为
     * YSM 左/右手模型。返回 true = 已渲染自定义模型，取消 vanilla {@code renderPlayerHand}。
     */
    @Unique
    private static boolean ysm$renderArm(HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector collector, int packedLight) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        // renderItemInHand 在 LevelRenderer#render 之后执行，firstPerson 标志已清除；
        // HandItemRenderer → processAnimation 依赖该标志（1.20.1 中手部渲染发生在 renderLevel 内）。
        boolean prev = ModelPreviewRenderer.isFirstPerson();
        ModelPreviewRenderer.setFirstPersonMode(true);
        boolean replaced;
        try {
            GeoBufferSource bufferSource = new GeoBufferSource();
            replaced = ReplacePlayerHandRenderEvent.onRenderArm(player, arm, poseStack, bufferSource, packedLight);
            if (replaced) {
                bufferSource.flush(collector, poseStack);
            }
        } finally {
            ModelPreviewRenderer.setFirstPersonMode(prev);
        }
        return replaced;
    }

    @Inject(method = "renderPlayerHand", at = @At("HEAD"), cancellable = true)
    private void ysm$onRenderHand(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight, HumanoidArm arm, PlayerRenderState playerRenderState, CallbackInfo ci) {
        if (ysm$renderArm(arm, poseStack, submitNodeCollector, packedLight)) {
            ci.cancel();
        }
    }

    /**
     * 对应 1.20.1 forge {@code RenderFirstPlayerForgeHook#onRenderHand}（RenderHandEvent →
     * RenderFirstPlayerBackground）：每帧一次提交带自定义肢体模型的 background（遮盖原版手臂）。
     * resetFrame 移到这里：26.3 中 {@code submitHandsWithItems} 与旧 RenderHandEvent 一样
     * 每帧仅此一次入口。
     */
    @Inject(method = "submitHandsWithItems", at = @At("HEAD"))
    private void ysm$onSubmitHandsWithItems(float partialTick, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, PlayerRenderState playerRenderState, net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState handsState, CallbackInfo ci) {
        AvatarRenderState avatarState = playerRenderState != null ? playerRenderState.avatarRenderState : null;
        // 1.20.1 RenderHandEvent 的 packedLight；26.3 首人称手臂光坐标来自 avatarRenderState.lightCoords。
        int packedLight = avatarState != null ? avatarState.lightCoords : 0xF000F0;
        RenderFirstPlayerBackground.resetFrame();
        RenderFirstPlayerBackground.onRenderHand(poseStack, submitNodeCollector, packedLight, partialTick);
    }
}
