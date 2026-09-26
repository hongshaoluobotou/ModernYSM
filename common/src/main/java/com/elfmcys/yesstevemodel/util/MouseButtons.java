package com.elfmcys.yesstevemodel.util;

/**
 * 26.3（SDL）鼠标键编号常量。1.20.1（GLFW）为 0=左键、1=右键、2=中键；26.3 换 SDL 后
 * {@code MouseHandler.onButton} / {@code MouseButtonEvent.button()} 使用 SDL 编号
 * 1=左键、2=中键、3=右键（字节码实证：onButton 按 button==1/2/3 分别置
 * isLeftPressed/isMiddlePressed/isRightPressed）。移植时所有 Screen 内的
 * {@code event.button()} 比较必须用本常量替换 GLFW 时代的魔法数字，否则左键判定永不成立、
 * 右键判定会误命中左键。
 */
public final class MouseButtons {

    public static final int LEFT = 1;
    public static final int MIDDLE = 2;
    public static final int RIGHT = 3;

    private MouseButtons() {
    }
}
