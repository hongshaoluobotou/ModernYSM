package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.client.event.ClientRawInputBridge;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V", at = @At("HEAD"))
    private void ysm$onButton(long window, net.minecraft.client.input.MouseButtonInfo info, int action, CallbackInfo ci) {
        ClientRawInputBridge.MOUSE_CLICKED_PRE.invoker().onMouseClicked(info.button(), action, info.modifiers());
    }
}
