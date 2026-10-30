package com.elfmcys.yesstevemodel.neoforge.client;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.client.renderer.AnimationDebugOverlay;
import com.elfmcys.yesstevemodel.client.renderer.ExtraPlayerOverlay;
import com.elfmcys.yesstevemodel.client.renderer.ModelSyncStateOverlay;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
import com.elfmcys.yesstevemodel.client.renderer.YsmRenderPipelines;
import com.elfmcys.yesstevemodel.client.renderer.YsmRenderTypes;
import com.elfmcys.yesstevemodel.client.renderer.pip.YsmPreviewRenderer;
import com.elfmcys.yesstevemodel.client.renderer.pip.YsmPreviewRenderState;
import com.elfmcys.yesstevemodel.neoforge.NeoForgeClientCommands;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import rip.ysm.api.client.HudRenderer;
import rip.ysm.api.impl.ClientResourceImpl;
import rip.ysm.api.impl.HudImpl;
import rip.ysm.api.impl.KeyMappingRegistryImpl;

/**
 * NeoForge 客户端初始化。
 *
 * <p>本类只会在物理客户端被加载（调用点由 {@code FMLEnvironment.getDist().isClient()} 守卫）。
 * 顺序与 fabric 的 {@code YesSteveModelFabricClient} 一致，唯一的差别是三个 mod 总线事件
 * （资源重载 / HUD 层 / 键位）需要先挂转发监听（{@code init}），common 侧的注册随后入队、
 * 事件触发时统一落地。
 */
public final class YesSteveModelNeoForgeClient {

    private YesSteveModelNeoForgeClient() {
    }

    public static void init(IEventBus modBus) {
        // 客户端接收处理器（S2C 包）——payload 类本身保持加载器/端中立，实现类只在物理客户端加载
        com.elfmcys.yesstevemodel.network.ClientPayloadSink.install(
                new com.elfmcys.yesstevemodel.client.net.ClientPayloadSinkImpl());

        ClientResourceImpl.init(modBus);
        HudImpl.init(modBus);
        KeyMappingRegistryImpl.init(modBus);
        registerPictureInPictureRenderers(modBus);

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
     * 客户端侧命令（{@code openysm ...}）：NeoForge 的客户端命令源是 {@code CommandSourceStack}，
     * 与 fabric 的 {@code FabricClientCommandSource} 不兼容，故命令体在加载器模块内实现。
     */
    private static void registerClientCommands() {
        NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class,
                event -> NeoForgeClientCommands.register(event.getDispatcher()));
    }

    /**
     * YSM 自定义 PiP 预览状态（{@code YsmPreviewRenderState}）的渲染器注册。
     *
     * <p>vanilla 26.3 的 {@code GuiRenderer} 对每个 PiP 状态类只持有一个 renderer（一份纹理），
     * 同帧多个 YSM 模型预览会互相覆盖；NeoForge 已把该机制改成按状态池化
     * （{@code PictureInPictureRendererPool}）并要求自定义状态类在此事件里注册渲染器工厂，
     * 池化由 NeoForge 负责（fabric/forge 没有这个 patch，改用
     * {@code GuiRendererPreviewMixin} + 自建池，见 common 的 {@code YsmPreviewRendererPool}）。</p>
     */
    private static void registerPictureInPictureRenderers(IEventBus modBus) {
        modBus.addListener(RegisterPictureInPictureRenderersEvent.class,
                event -> event.register(YsmPreviewRenderState.class,
                        () -> new YsmPreviewRenderer(
                                net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher())));
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
