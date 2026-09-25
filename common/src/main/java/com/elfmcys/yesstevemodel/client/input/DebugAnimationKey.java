package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.renderer.AnimationDebugOverlay;
import com.elfmcys.yesstevemodel.util.InputUtil;
import com.mojang.blaze3d.platform.InputConstants;
import com.elfmcys.yesstevemodel.client.event.ClientRawInputBridge;
import net.minecraft.client.KeyMapping;
import rip.ysm.api.PlatformAPI;
import rip.ysm.api.client.KeyMappingFactory;

public final class DebugAnimationKey {

    public static final KeyMapping KEY_MAPPING = KeyMappingFactory.createInGameAlt("key.yes_steve_model.debug_animation.desc", InputConstants.Type.KEYBOARD, 5, "key.category.yes_steve_model");

    private DebugAnimationKey() {
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        ClientRawInputBridge.KEY_PRESSED.register((keyCode, scanCode, action, modifiers) -> {
            if (YesSteveModel.isAvailable() && InputUtil.isPlayerReady() && action == 1 && InputUtil.isKeyPressed(keyCode, scanCode, KEY_MAPPING)) {
                if (!AnimationDebugOverlay.isDebugActive()) {
                    AnimationDebugOverlay.tryUpdateFromHitResult();
                } else {
                    AnimationDebugOverlay.clearActiveModel();
                }
            }
        });
    }
}