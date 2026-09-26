package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.client.event.*;
import com.elfmcys.yesstevemodel.client.input.InputStateKey;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
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
            PlayerSkinTextureManager.register();
            // 26.3 port: RendererManager.register() 已在 fabric client entrypoint（YesSteveModelFabricClient）
            // 中注册（fabric ResourceLoader 禁止重复注册），勿在此重复调用。
            // 26.3 port: 各键位类（依赖 client.gui 屏幕）已随 GUI 恢复注册（见键位类文件）。
            InputStateKey.register();
        }
    }
}
