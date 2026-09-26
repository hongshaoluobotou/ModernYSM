package com.elfmcys.yesstevemodel;

import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.config.ModSoundEvents;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.event.YsmEventBootstrap;
import com.elfmcys.yesstevemodel.util.ServerInstanceHolder;
import com.elfmcys.yesstevemodel.util.obfuscate.Keep;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rip.ysm.api.PlatformAPI;
import rip.ysm.api.config.ConfigRegistration;

import java.io.File;

/**
 * TODO:
 * 默认模型应该就在模组架加载的时候就预加载了
 * 其它模型统统都是进入世界后加载
 */
public class YesSteveModel {
    public static final String MOD_ID = "yes_steve_model";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private YesSteveModel() {
    }

    public static void init() {
        LOGGER.info("Initializing YesSteveModel, platform: " + PlatformAPI.getPlatformName());
        ServerInstanceHolder.init();
        // 渲染管线原生适配（阶段①）：不再加载 ysm-core natives（GPU/SIMD 路径已删除，预编译产物为
        // 1.20.1 时代 JNI 签名，加载无意义）。NativeLibLoader 类保留（isOnAndroid / 错误文案通道 +
        // 将来 GPU 路径若重编 natives 可复用提取逻辑），但启动时不再调用 init()。
        initConfig();
        YsmEventBootstrap.register();
    }

    private static void initConfig() {
        var configDir = FabricLoader.getInstance().getConfigDir();
        File oldConfig = configDir.resolve("yes_steve_model-common.toml").toFile();
        if (oldConfig.isFile()) {
            File file2 = configDir.resolve("yes_steve_model-client.toml").toFile();
            if (!file2.isFile()) {
                oldConfig.renameTo(file2);
            } else {
                oldConfig.delete();
            }
        }
        ConfigRegistration.register(MOD_ID, GeneralConfig.buildSpec(configDir.resolve("yes_steve_model-client.toml")));
        ConfigRegistration.register(MOD_ID, ServerConfig.buildSpec(configDir.resolve("yes_steve_model-server.toml")));
        if (!PlatformAPI.isServer()) {
            ModSoundEvents.register();
        }
    }

    @Keep
    public static boolean isAvailable() {
        // 渲染管线原生适配（阶段①）：native 依赖已整体移除（GPU/SIMD 路径删除，见 AGENTS.md 路线图），
        // 本开关恒为 true。约 40 处消费点原语义为"native 库可用才启用 mod 功能"，移植期真机上 native
        // 加载本就失败（旧 JNI 签名不匹配），功能早已实际全开——现在把语义定死，消除"静默降级"歧义。
        // sendUnavailableMessage/getUnavailableComponent 等错误通道保留（永不触发的防御分支）。
        return true;
    }

    public static boolean isOnAndroid() {
        return NativeLibLoader.isOnAndroid();
    }

    @Environment(EnvType.CLIENT)
    public static void sendUnavailableMessage() {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null) {
            localPlayer.sendSystemMessage(getUnavailableComponent());
        }
    }

    public static Component getUnavailableComponent() {
        return NativeLibLoader.getErrorComponent();
    }

    public static String getErrorMessage() {
        return NativeLibLoader.getErrorMessage();
    }
}
