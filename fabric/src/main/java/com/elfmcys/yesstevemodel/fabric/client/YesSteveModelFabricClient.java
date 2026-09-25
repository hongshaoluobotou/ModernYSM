package com.elfmcys.yesstevemodel.fabric.client;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.client.renderer.AnimationDebugOverlay;
import com.elfmcys.yesstevemodel.client.renderer.ModelSyncStateOverlay;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

// 26.3 port: HUD 由 HudRenderCallback（已移除）改为 HudElementRegistry（GuiGraphicsExtractor 体系）；
// 渲染器注册链（RendererManager.reload listener）恢复。
public final class YesSteveModelFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientModelManager.loadDefaultModel();
        RendererManager.register();
        DebugAnimationKey.register();
        // HUD：动画调试 / 模型同步状态（26.3 HudElement API）
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("yes_steve_model", "animation_debug"),
                AnimationDebugOverlay.createHudElement());
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("yes_steve_model", "model_sync_state"),
                new ModelSyncStateOverlay());
    }
}
