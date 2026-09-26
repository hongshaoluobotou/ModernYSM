package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.client.event.ClientRawInputBridge;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V", at = @At("HEAD"), cancellable = true)
    private void ysm$keyPress(long window, int action, KeyEvent event, CallbackInfo ci) {
        // 26.3（SDL）：KeyMapping.matches(KeyEvent) 比较的是 event.key()（scancode 域，与 KeyMapping.key 同域，
        // isKeyDown 也用该值索引键盘缓冲）；event.keycode() 是布局相关的 keycode，与 matches 不同域。
        // 桥接约定的 (keyCode, scanCode) 参数：(keyCode=event.key(), scanCode=event.keycode())。
        Screen before = this.minecraft.gui.screen();
        ClientRawInputBridge.KEY_PRESSED.invoker().onKeyPressed(event.key(), event.keycode(), action, event.modifiers());
        // 若桥接监听器（如 PlayerModelToggleKey）在本次事件中打开了 Screen，则取消原版后续处理：
        // 原版 keyPress 在 HEAD 之后才读取 gui.screen()，会把同一个按键事件转发给新打开的 Screen，
        // 而 PlayerModelScreen#handleToggleKey 会对匹配的 Y 键执行 onClose() —— 导致界面"打开即关闭"。
        if (this.minecraft.gui.screen() != before) {
            ci.cancel();
        }
    }
}
