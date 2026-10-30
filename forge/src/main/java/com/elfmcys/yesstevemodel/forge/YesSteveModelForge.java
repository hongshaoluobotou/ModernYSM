package com.elfmcys.yesstevemodel.forge;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.forge.client.YesSteveModelForgeClient;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Forge 主入口点。
 *
 * <p>顺序要求（与 NeoForge 版一致）：
 * <ol>
 *   <li>先把 mod BusGroup 存进 {@link ForgeModBus}——{@code YesSteveModel.init()} 内部的
 *       {@code RegisterEvent} 注册拿不到构造器参数；</li>
 *   <li>再跑 common 初始化（配置 / 事件 / 命令 / 网络）；</li>
 *   <li>客户端专属初始化放在独立类里、用 {@code FMLEnvironment.dist.isClient()} 守卫
 *       （{@code @Mod} 注解本身没有端信息，专用服务器绝不能加载客户端类）。</li>
 * </ol>
 */
@Mod(YesSteveModel.MOD_ID)
public final class YesSteveModelForge {

    public YesSteveModelForge() {
        ForgeModBus.set(FMLJavaModLoadingContext.get().getModBusGroup());
        YesSteveModel.init();
        ForgeCapabilityHooks.register();
        if (FMLEnvironment.dist.isClient()) {
            YesSteveModelForgeClient.init();
        }
    }
}
