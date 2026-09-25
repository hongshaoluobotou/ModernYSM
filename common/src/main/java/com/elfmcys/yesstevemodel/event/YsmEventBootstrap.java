package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.client.event.*;
import com.elfmcys.yesstevemodel.client.input.InputStateKey;
import rip.ysm.api.PlatformAPI;

public final class YsmEventBootstrap {

    private YsmEventBootstrap() {
    }

    public static void register() {
        ServerStartupEvent.register();
        EnterServerEvent.register();
        PlayerLogoutEvent.register();
        CommonEvent.register();
        CommandRegistry.register();
        CapabilityEvent.register();
        if (!PlatformAPI.isServer()) {
            EntityJoinCallbackEvent.register();
            ClientSetupEvent.register();
            ClientTickEvent.register();
            ClientPlayerJoinNotification.register();
            ClientPlayerCloneEvent.register();
            AnimationLockEvent.register();
            // TODO port 26.3: PlayerSkinTextureManager / RendererManager（client.renderer）以及
            // 各键位类（依赖 client.gui 屏幕）仍在渲染层排除区，恢复后在此补回注册。
            InputStateKey.register();
        }
    }
}
