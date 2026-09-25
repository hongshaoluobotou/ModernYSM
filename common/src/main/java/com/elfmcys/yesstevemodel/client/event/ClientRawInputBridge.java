package com.elfmcys.yesstevemodel.client.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * 原 Architectury ClientRawInputEvent 的替代：
 * 通过 mixin 到 KeyboardHandler#keyPress 与 MouseHandler#onButton 捕获原始键鼠输入。
 * KEY_PRESSED 回调参数为 (keyCode, scanCode, action, modifiers)；
 * MOUSE_CLICKED_PRE 回调参数为 (button, action, modifiers)。
 * action 含义与 GLFW 一致：1=按下，0=释放。
 */
public final class ClientRawInputBridge {

    public static final Event<KeyPressed> KEY_PRESSED = EventFactory.createArrayBacked(KeyPressed.class,
            listeners -> (keyCode, scanCode, action, modifiers) -> {
                for (KeyPressed listener : listeners) {
                    listener.onKeyPressed(keyCode, scanCode, action, modifiers);
                }
            });

    public static final Event<MouseClicked> MOUSE_CLICKED_PRE = EventFactory.createArrayBacked(MouseClicked.class,
            listeners -> (button, action, modifiers) -> {
                for (MouseClicked listener : listeners) {
                    listener.onMouseClicked(button, action, modifiers);
                }
            });

    @FunctionalInterface
    public interface KeyPressed {
        void onKeyPressed(int keyCode, int scanCode, int action, int modifiers);
    }

    @FunctionalInterface
    public interface MouseClicked {
        void onMouseClicked(int button, int action, int modifiers);
    }

    private ClientRawInputBridge() {
    }
}
