package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.util.InputUtil;
import com.elfmcys.yesstevemodel.client.event.ClientRawInputBridge;
import rip.ysm.api.PlatformAPI;

public class InputStateKey {

    /**
     * 键盘状态表，索引域 = SDL scancode（26.3 原生，无 GLFW 换算）。
     * 尺寸取 SDL 标准常量 SDL_SCANCODE_COUNT = 512。
     * 即 molang {@code input_key_down(N)} 的 N 为 SDL scancode（A=4、Y=28、Z=29、
     * 数字 1=30、Esc=41、Space=44、Shift=225/229 等）。
     * <p>
     * 破坏性变更（相对 1.20.1）：1.20.1 模型包按 GLFW 键码（A=65 起）书写，
     * 26.3 起需按 SDL scancode 书写（GLFW→SDL：字母/数字 = GLFW-61/19，
     * 详见 AGENTS.md 迁移对照表）。
     */
    public static volatile boolean[] keyStates = new boolean[512];

    /**
     * 鼠标状态表，索引域 = SDL 鼠标键编号（与 util/MouseButtons 一致：左=1、中=2、
     * 右=3，侧键 4..8）。即 molang {@code mouse_key_down(N)} 的 N 为 SDL 编号。
     * <p>
     * 破坏性变更（相对 1.20.1）：1.20.1 为 GLFW 编号（左=0、右=1、中=2）。
     * 索引 0 恒为 false（SDL 无 0 号键），保留以兼容越界安全读取。
     */
    public static volatile boolean[] mouseStates = new boolean[9];

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

    private static void onKeyInput(int scancode, int action) {
        // 26.3 原生 SDL 域：scancode 即 KeyEvent.key()（SDL scancode），直接存储，
        // 不做任何 GLFW 换算。模型包需按 SDL scancode 书写 input_key_down。
        if (YesSteveModel.isAvailable() && InputUtil.isPlayerReady() && 0 <= scancode && scancode < keyStates.length) {
            if (action == 1) {
                keyStates[scancode] = true;
            } else if (action == 0) {
                keyStates[scancode] = false;
            }
        }
    }

    private static void onMouseInput(int button, int action) {
        // 26.3 原生 SDL 域：直接存储 MouseButtonEvent.button() 的 SDL 编号
        // （左=1/中=2/右=3），不做 GLFW 换算。
        if (YesSteveModel.isAvailable() && InputUtil.isPlayerReady() && 0 <= button && button < mouseStates.length) {
            if (action == 1) {
                mouseStates[button] = true;
            } else if (action == 0) {
                mouseStates[button] = false;
            }
        }
    }
}
