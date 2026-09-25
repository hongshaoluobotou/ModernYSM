package com.elfmcys.yesstevemodel.fabric.client;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import net.fabricmc.api.ClientModInitializer;

// TODO port: 26.3 渲染管线迁移后恢复 HUD 注册（HudRenderCallback 已改名/移除，需评估 HudLayerRegistrationCallback）
// 以及 AnimationDebugOverlay / ExtraPlayerOverlay / ModelSyncStateOverlay 的注册。
public final class YesSteveModelFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientModelManager.loadDefaultModel();
    }
}
