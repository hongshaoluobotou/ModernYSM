package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.client.event.ClientRawInputBridge;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V", at = @At("HEAD"))
    private void ysm$keyPress(long window, int action, KeyEvent event, CallbackInfo ci) {
        // 26.3（SDL）：KeyMapping.matches(KeyEvent) 比较的是 event.key()（scancode 域，与 KeyMapping.key 同域，
        // isKeyDown 也用该值索引键盘缓冲）；event.keycode() 是布局相关的 keycode，与 matches 不同域。
        // 桥接约定的 (keyCode, scanCode) 参数：(keyCode=event.key(), scanCode=event.keycode())。
        ClientRawInputBridge.KEY_PRESSED.invoker().onKeyPressed(event.key(), event.keycode(), action, event.modifiers());
    }
}
