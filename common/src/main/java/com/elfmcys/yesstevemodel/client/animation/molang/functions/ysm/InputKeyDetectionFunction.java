package com.elfmcys.yesstevemodel.client.animation.molang.functions.ysm;

import com.elfmcys.yesstevemodel.util.InputUtil;
import com.elfmcys.yesstevemodel.client.input.InputStateKey;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.molang.runtime.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InputKeyDetectionFunction {

    /**
     * molang {@code input_key_down(N...)}：N 为 SDL scancode（26.3 原生域，无 GLFW 换算）。
     * 破坏性变更：1.20.1 模型包按 GLFW 键码（A=65 起）书写，26.3 起需改为 SDL scancode
     * （A=4、Y=28、Z=29、数字 1=30、Esc=41、Space=44 等，见 AGENTS.md 迁移对照表）。
     */
    public static class Keyboard implements Function {
        @Override
        @Nullable
        public Object evaluate(@NotNull ExecutionContext<?> context, @NotNull Function.ArgumentCollection arguments) {
            if (!InputUtil.isPlayerReady()) {
                return false;
            }
            for (int i = 0; i < arguments.size(); i++) {
                int scancode = arguments.getAsInt(context, i);
                if (0 <= scancode && scancode < InputStateKey.keyStates.length && InputStateKey.keyStates[scancode]) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean validateArgumentSize(int size) {
            return size >= 1;
        }
    }

    /**
     * molang {@code mouse_key_down(N)}：N 为 SDL 鼠标键编号（左=1、中=2、右=3、
     * 侧键 4..8，与 util/MouseButtons 一致）。
     * 破坏性变更：1.20.1 为 GLFW 编号（左=0、右=1、中=2）。
     */
    public static class Mouse implements Function {
        @Override
        @Nullable
        public Object evaluate(@NotNull ExecutionContext<?> context, @NotNull Function.ArgumentCollection arguments) {
            if (!InputUtil.isPlayerReady()) {
                return false;
            }
            int button = arguments.getAsInt(context, 0);
            if (0 <= button && button < InputStateKey.mouseStates.length) {
                return InputStateKey.mouseStates[button];
            }
            return false;
        }

        @Override
        public boolean validateArgumentSize(int size) {
            return size == 1;
        }
    }
}