package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.util.CameraUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.player.Player;
import rip.ysm.compat.firstperson.FirstPersonCompat;
import rip.ysm.compat.playeranimator.PlayerAnimatorCompat;
import rip.ysm.compat.realcamera.RealCameraCompat;

// 26.3 port: 原 MultiBufferSource 立即渲染改为 SubmitNodeCollector 提交体系。
public class ReplacePlayerRenderEvent {

    private ReplacePlayerRenderEvent() {
    }

    public static boolean onRenderPlayerPre(Player entity, float partialTick, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, AvatarRenderState vanillaState) {
        if (!YesSteveModel.isAvailable()) {
            return false;
        }
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (entity.equals(localPlayer) && GeneralConfig.DISABLE_SELF_MODEL.get().booleanValue()) {
            return false;
        }
        // 出窍（tweakeroo 等 freecam 换 cameraEntity）时本地玩家会被 spoof 成 spectator
        // （tweakeroo MixinPlayer_freeCam 在整帧内令 isSpectator()==true 以影响剔除/手部），
        // 身体实际从外部可见，不能因此跳过 YSM 接管；zergatul freecam 只在 Camera.extractRenderState
        // 期间 spoof，不波及实体渲染路径，无需此豁免。
        if ((!entity.equals(localPlayer) && GeneralConfig.DISABLE_OTHER_MODEL.get().booleanValue()) || (entity.isSpectator() && !CameraUtil.isCameraDetached(entity))) {
            return false;
        }
        boolean[] cancelled = {false};
        PlayerCapability.get(entity).ifPresent(cap -> {
            if (cap.isModelActive()) {
                if (!CameraUtil.isFirstPerson(cap)
                        || FirstPersonCompat.isFirstPersonActive()
                        || RealCameraCompat.isActive()
                        || GeneralConfig.DISABLE_EXTERNAL_FP_ANIM.get().booleanValue()
                        || !PlayerAnimatorCompat.isPlayerAnimated(localPlayer)) {
                    cancelled[0] = RendererManager.getPlayerRenderer().renderPlayer(entity, partialTick, poseStack, submitNodeCollector, camera, vanillaState);
                }
            }
        });
        return cancelled[0];
    }
}
