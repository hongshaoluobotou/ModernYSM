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
        // 26.3 中 KeyEvent 的 keycode() 为 GLFW 键码、key() 为 scancode
        ClientRawInputBridge.KEY_PRESSED.invoker().onKeyPressed(event.keycode(), event.key(), action, event.modifiers());
    }
}
