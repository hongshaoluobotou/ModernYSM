package com.elfmcys.yesstevemodel.neoforge;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.neoforge.client.YesSteveModelNeoForgeClient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * NeoForge 主入口点。
 *
 * <p>顺序要求：
 * <ol>
 *   <li>先把 mod 总线存进 {@link NeoForgeModBus}——{@code YesSteveModel.init()} 内部的网络层注册
 *       （{@code RegisterPayloadHandlersEvent}）拿不到构造器参数；</li>
 *   <li>再跑 common 初始化（配置 / 事件 / 命令 / 网络）；</li>
 *   <li>客户端专属初始化放在独立的类里、用 {@code FMLEnvironment.getDist().isClient()} 守卫，
 *       保证专用服务器上不会加载任何客户端类（{@code @Environment} 注解没有运行时效果）。</li>
 * </ol>
 */
@Mod(YesSteveModel.MOD_ID)
public final class YesSteveModelNeoForge {

    public YesSteveModelNeoForge(IEventBus modBus) {
        NeoForgeModBus.set(modBus);
        YesSteveModel.init();
        NeoForgeCapabilityHooks.register();
        if (FMLEnvironment.getDist().isClient()) {
            YesSteveModelNeoForgeClient.init(modBus);
        }
    }
}
