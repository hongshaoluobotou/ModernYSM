package com.elfmcys.yesstevemodel.fabric.client;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.client.renderer.AnimationDebugOverlay;
import com.elfmcys.yesstevemodel.client.renderer.ExtraPlayerOverlay;
import com.elfmcys.yesstevemodel.client.renderer.ModelSyncStateOverlay;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
import com.elfmcys.yesstevemodel.client.renderer.YsmRenderPipelines;
import com.elfmcys.yesstevemodel.client.renderer.YsmRenderTypes;
import com.elfmcys.yesstevemodel.command.OpenYSMClientCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.resources.Identifier;
import rip.ysm.api.client.HudRenderer;

// HUD 体系为 26.3 GuiGraphicsExtractor（提取式）；具体注册经 rip.ysm.api.client.HudRenderer
// 抽象落到 fabric 的 HudElementRegistry（见 rip.ysm.api.impl.HudImpl）。
public final class YesSteveModelFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 客户端接收处理器（S2C 包）——payload 类本身保持加载器/端中立，实现类只在物理客户端加载
        com.elfmcys.yesstevemodel.network.ClientPayloadSink.install(
                new com.elfmcys.yesstevemodel.client.net.ClientPayloadSinkImpl());
        // 阶段②：尽早注册 YSM 自定义 RenderPipeline，进入 vanilla 启动期 shader 编译/预热清单。
        // Iris 兼容模式（YsmRenderTypes.IRIS_LOADED）下不注册自定义管线，RenderType 全部回退 vanilla。
        if (!YsmRenderTypes.IRIS_LOADED) {
            YsmRenderPipelines.init();
        }
        ClientModelManager.loadDefaultModel();
        RendererManager.register();
        DebugAnimationKey.register();
        registerClientCommands();
        registerHudElements();
    }

    /**
     * 客户端侧命令（{@code openysm ...}）：Fabric 的客户端命令源是 {@code FabricClientCommandSource}，
     * 与 NeoForge/Forge 的 {@code CommandSourceStack} 不兼容，故由加载器模块各自注册，
     * 不进 common 的 {@code CommandEvents} 抽象。
     */
    private void registerClientCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) ->
                OpenYSMClientCommand.registerClientCommands(dispatcher));
    }

    private void registerHudElements() {
        HudRenderer.register(Identifier.fromNamespaceAndPath("yes_steve_model", "animation_debug"),
                AnimationDebugOverlay.createHudElement());
        HudRenderer.register(Identifier.fromNamespaceAndPath("yes_steve_model", "model_sync_state"),
                new ModelSyncStateOverlay());
        // 额外玩家渲染纸娃娃（经 renderPlayerOverlay → renderFixed PiP）
        HudRenderer.register(Identifier.fromNamespaceAndPath("yes_steve_model", "extra_player_render"),
                new ExtraPlayerOverlay());
    }
}
