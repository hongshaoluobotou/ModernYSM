package com.elfmcys.yesstevemodel.forge.client;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.client.renderer.AnimationDebugOverlay;
import com.elfmcys.yesstevemodel.client.renderer.ExtraPlayerOverlay;
import com.elfmcys.yesstevemodel.client.renderer.ModelSyncStateOverlay;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
import com.elfmcys.yesstevemodel.client.renderer.YsmRenderPipelines;
import com.elfmcys.yesstevemodel.client.renderer.YsmRenderTypes;
import com.elfmcys.yesstevemodel.forge.ForgeClientCommands;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import rip.ysm.api.client.HudRenderer;
import rip.ysm.api.impl.ClientResourceImpl;
import rip.ysm.api.impl.HudImpl;
import rip.ysm.api.impl.KeyMappingRegistryImpl;

/**
 * Forge 客户端初始化（只会在物理客户端被加载）。
 *
 * <p>顺序与 fabric/neoforge 版一致：先挂好三个客户端事件的「入队 → 落地」监听，
 * 再跑 common 侧注册（资源重载监听 / 键位 / HUD 层），最后注册客户端命令。
 */
public final class YesSteveModelForgeClient {

    private YesSteveModelForgeClient() {
    }

    public static void init() {
        // 客户端接收处理器（S2C 包）——payload 类保持端中立，实现类只在物理客户端加载
        com.elfmcys.yesstevemodel.network.ClientPayloadSink.install(
                new com.elfmcys.yesstevemodel.client.net.ClientPayloadSinkImpl());

        ClientResourceImpl.init();
        HudImpl.init();
        KeyMappingRegistryImpl.init();

        if (!YsmRenderTypes.IRIS_LOADED) {
            YsmRenderPipelines.init();
        }
        ClientModelManager.loadDefaultModel();
        RendererManager.register();
        DebugAnimationKey.register();
        registerClientCommands();
        registerHudElements();
    }

    private static void registerClientCommands() {
        RegisterClientCommandsEvent.BUS.addListener(event -> ForgeClientCommands.register(event.getDispatcher()));
    }

    private static void registerHudElements() {
        HudRenderer.register(Identifier.fromNamespaceAndPath("yes_steve_model", "animation_debug"),
                AnimationDebugOverlay.createHudElement());
        HudRenderer.register(Identifier.fromNamespaceAndPath("yes_steve_model", "model_sync_state"),
                new ModelSyncStateOverlay());
        // 额外玩家渲染纸娃娃（经 renderPlayerOverlay → renderFixed PiP）
        HudRenderer.register(Identifier.fromNamespaceAndPath("yes_steve_model", "extra_player_render"),
                new ExtraPlayerOverlay());
    }
}
