package com.elfmcys.yesstevemodel.fabric.client;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.client.renderer.AnimationDebugOverlay;
import com.elfmcys.yesstevemodel.client.renderer.ExtraPlayerOverlay;
import com.elfmcys.yesstevemodel.client.renderer.ModelSyncStateOverlay;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
import com.elfmcys.yesstevemodel.client.renderer.YsmRenderPipelines;
import com.elfmcys.yesstevemodel.client.renderer.YsmRenderTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

// 26.3 port: HUD 由 HudRenderCallback（已移除）改为 HudElementRegistry（GuiGraphicsExtractor 体系）；
// 渲染器注册链（RendererManager.reload listener）恢复。
public final class YesSteveModelFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 阶段②：尽早注册 YSM 自定义 RenderPipeline，进入 vanilla 启动期 shader 编译/预热清单。
        // Iris 兼容模式（YsmRenderTypes.IRIS_LOADED）下不注册自定义管线，RenderType 全部回退 vanilla。
        if (!YsmRenderTypes.IRIS_LOADED) {
            YsmRenderPipelines.init();
        }
        ClientModelManager.loadDefaultModel();
        RendererManager.register();
        DebugAnimationKey.register();
        // HUD：动画调试 / 模型同步状态（26.3 HudElement API）
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("yes_steve_model", "animation_debug"),
                AnimationDebugOverlay.createHudElement());
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("yes_steve_model", "model_sync_state"),
                new ModelSyncStateOverlay());
        // HUD：额外玩家渲染纸娃娃（26.3 HudElement API，经 renderPlayerOverlay → renderFixed PiP）
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("yes_steve_model", "extra_player_render"),
                new ExtraPlayerOverlay());
    }
}