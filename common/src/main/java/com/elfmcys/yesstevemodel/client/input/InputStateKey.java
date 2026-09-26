package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.util.InputUtil;
import com.elfmcys.yesstevemodel.client.event.ClientRawInputBridge;
import rip.ysm.api.PlatformAPI;

public class InputStateKey {

    public static volatile boolean[] keyStates = new boolean[349];

    public static volatile boolean[] mouseStates = new boolean[8];

    private InputStateKey() {
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        ClientRawInputBridge.KEY_PRESSED.register((keyCode, scanCode, action, modifiers) -> {
            onKeyInput(keyCode, action);
        });
        ClientRawInputBridge.MOUSE_CLICKED_PRE.register((button, action, modifiers) -> {
            onMouseInput(button, action);
        });
    }

    private static void onKeyInput(int keyCode, int action) {
        if (YesSteveModel.isAvailable() && InputUtil.isPlayerReady() && 32 <= keyCode && keyCode <= 348) {
            if (action == 1) {
                keyStates[keyCode] = true;
            } else if (action == 0) {
                keyStates[keyCode] = false;
            }
        }
    }

    private static void onMouseInput(int button, int action) {
        // 26.3 port（SDL）：MouseHandlerMixin 传入的是 SDL 键编号（1=左、2=中、3=右），
        // 而 mouseStates 的索引语义 = molang `mouse_key_down(N)` 的 N，即 1.20.1（GLFW）
        // 编号（0=左、1=右、2=中）——模型包均按 GLFW 语义书写。此处统一换算回 GLFW 编号
        // 存储（SDL 4..8 侧键依次对应 GLFW 3..7），保持模型包输入查询的向后兼容。
        int glfwButton = switch (button) {
            case 1 -> 0; // SDL 左键 → GLFW 0
            case 2 -> 2; // SDL 中键 → GLFW 2
            case 3 -> 1; // SDL 右键 → GLFW 1
            default -> button - 1; // SDL 4..8 侧键 → GLFW 3..7
        };
        if (YesSteveModel.isAvailable() && InputUtil.isPlayerReady() && 0 <= glfwButton && glfwButton <= 7) {
            if (action == 1) {
                mouseStates[glfwButton] = true;
            } else if (action == 0) {
                mouseStates[glfwButton] = false;
            }
        }
    }
}
