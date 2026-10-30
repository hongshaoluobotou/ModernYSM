package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.C2SPlayAnimationPacket;
import com.elfmcys.yesstevemodel.client.event.ClientRawInputBridge;
import com.elfmcys.yesstevemodel.client.input.AnimationRouletteKey;
import com.elfmcys.yesstevemodel.util.InputUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import rip.ysm.api.PlatformAPI;
import rip.ysm.api.event.ClientEvents;

public class AnimationLockEvent {

    private static boolean animationLocked = false;

    private AnimationLockEvent() {
    }

    public static void register() {
        // 26.3 port（2026.11.02 恢复）：1.20.1 的 ClientRawInputEvent.KEY_PRESSED 在 26.3 由
        // ClientRawInputBridge 承接（KeyboardHandlerMixin → KEY_PRESSED，参数仍是 GLFW 语义的
        // (keyCode, scanCode, action, modifiers)，其中 keyCode/scanCode 分别来自 KeyEvent.key()/
        // keycode()，即 SDL scancode / 布局键码两域，KeyMappingFactory.isActiveAndMatches 按
        // 26.3 KEYBOARD 域匹配），故可直接复用 InputUtil.isKeyPressed 判定 KEY_LOCK。
        ClientRawInputBridge.KEY_PRESSED.register((keyCode, scanCode, action, modifiers) -> {
            if (YesSteveModel.isAvailable() && action == 1
                    && InputUtil.isKeyPressed(keyCode, scanCode, AnimationRouletteKey.KEY_LOCK)) {
                animationLocked = !animationLocked;
            }
        });
        ClientEvents.onTick(() -> onClientTick(Minecraft.getInstance()));
    }

    private static void onClientTick(Minecraft client) {
        LocalPlayer localPlayer;
        if (YesSteveModel.isAvailable() && !animationLocked && (localPlayer = client.player) != null && isPlayerMoving(localPlayer)) {
            PlayerCapability.get(localPlayer).ifPresent(cap -> {
                if (cap.isModelSwitching()) {
                    cap.clearModelSwitch();
                    if (NetworkHandler.isClientConnected()) {
                        NetworkHandler.sendToServer(C2SPlayAnimationPacket.createDefault());
                    }
                }
            });
        }
    }

    public static boolean isPlayerMoving(LocalPlayer localPlayer) {
        // TODO port: 26.3 Input 改为 record，键入状态经 ClientInput.keyPresses
        net.minecraft.client.player.ClientInput input = localPlayer.input;
        return input != null && (isSignificantImpulse(input.getMoveVector().x) || isSignificantImpulse(input.getMoveVector().y) || input.keyPresses.jump() || input.keyPresses.shift());
    }

    private static boolean isSignificantImpulse(float impulse) {
        return Math.abs(impulse) > 1.0E-5f;
    }

    public static void toggleLock() {
        animationLocked = !animationLocked;
    }

    public static boolean isLocked() {
        return animationLocked;
    }
}