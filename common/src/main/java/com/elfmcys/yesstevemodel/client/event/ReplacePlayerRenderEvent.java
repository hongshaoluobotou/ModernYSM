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
        if ((!entity.equals(localPlayer) && GeneralConfig.DISABLE_OTHER_MODEL.get().booleanValue()) || entity.isSpectator()) {
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
