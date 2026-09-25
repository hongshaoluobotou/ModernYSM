package com.elfmcys.yesstevemodel.util;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import rip.ysm.api.client.KeyMappingFactory;

public class InputUtil {
    public static boolean isKeyPressed(int keyCode, int scanCode, KeyMapping keyMapping) {
        return KeyMappingFactory.isActiveAndMatches(keyMapping, keyCode, scanCode);
    }

    public static boolean isPlayerReady() {
        Minecraft minecraft = Minecraft.getInstance();
        // TODO port: 26.3 Minecraft#getOverlay 移除，仅以 screen + 鼠标锁定判断
        if (minecraft.screen != null || !minecraft.mouseHandler.isMouseGrabbed()) {
            return false;
        }
        return minecraft.isWindowActive();
    }
}