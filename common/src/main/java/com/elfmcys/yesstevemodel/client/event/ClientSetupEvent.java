package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.animation.AnimationRegister;
import com.elfmcys.yesstevemodel.client.gui.DisclaimerScreen;
import com.elfmcys.yesstevemodel.client.input.AnimationRouletteKey;
import com.elfmcys.yesstevemodel.client.input.ExtraAnimationKey;
import com.elfmcys.yesstevemodel.client.input.ExtraPlayerRenderKey;
import com.elfmcys.yesstevemodel.client.input.PlayerModelToggleKey;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.network.chat.Component;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import rip.ysm.api.PlatformAPI;
import net.minecraft.client.KeyMapping;

public final class ClientSetupEvent {

    private ClientSetupEvent() {
    }

    public static void register() {
        registerKeyMappings();
        // 键位事件监听（ClientRawInputBridge）：1.20.1 由各键位类在客户端事件总线自注册，
        // 26.3 移植后统一在此挂接——此前只有 DebugAnimationKey（fabric client entrypoint）挂了，
        // PlayerModelToggleKey 等监听从未注册导致 Y 键无效。
        PlayerModelToggleKey.register();
        AnimationRouletteKey.register();
        ExtraPlayerRenderKey.register();
        ExtraAnimationKey.register();
        if (YesSteveModel.isAvailable()) {
            AnimationRegister.registerAnimationState();
        }
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            if (!YesSteveModel.isAvailable()) {
                return;
            }
            checkNativeInitialization();
        });
        // 临时诊断：确认键位是否进入 Options.keyMappings（真机排查"绑定界面无 mod 键位"）
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            int found = 0;
            for (KeyMapping km : client.options.keyMappings) {
                if (km.getName() != null && km.getName().startsWith("key.yes_steve_model")) {
                    found++;
                    YesSteveModel.LOGGER.info("[YSM diag] keyMapping in Options: {} bound={}", km.getName(), km.isUnbound() ? "unbound" : "bound");
                }
            }
            YesSteveModel.LOGGER.info("[YSM diag] {} YSM keyMappings present in Options (total {})", found, client.options.keyMappings.length);
        });
    }

    private static void registerKeyMappings() {
        // TODO port 26.3: 以下键位类依赖 client.gui 屏幕（渲染层排除区），恢复后可改回直接引用。
        registerKeyMappingIfPresent("com.elfmcys.yesstevemodel.client.input.PlayerModelToggleKey", "KEY_MAPPING");
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        registerKeyMappingIfPresent("com.elfmcys.yesstevemodel.client.input.AnimationRouletteKey", "KEY_ROULETTE");
        registerKeyMappingIfPresent("com.elfmcys.yesstevemodel.client.input.AnimationRouletteKey", "KEY_LOCK");
        registerKeyMappingIfPresent("com.elfmcys.yesstevemodel.client.input.DebugAnimationKey", "KEY_MAPPING");
        registerKeyMappingIfPresent("com.elfmcys.yesstevemodel.client.input.ExtraPlayerRenderKey", "KEY_MAPPING");
        try {
            for (KeyMapping mapping : (KeyMapping[]) Class
                    .forName("com.elfmcys.yesstevemodel.client.input.ExtraAnimationKey")
                    .getMethod("getKeyMappings").invoke(null)) {
                KeyMappingHelper.registerKeyMapping(mapping);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void registerKeyMappingIfPresent(String className, String fieldName) {
        try {
            KeyMappingHelper.registerKeyMapping((KeyMapping) Class.forName(className).getField(fieldName).get(null));
        } catch (Throwable t) {
            YesSteveModel.LOGGER.error("Failed to register key mapping {}.{}", className, fieldName, t);
        }
    }

    public static Object nativeClientInit() {
        try {
            int maxTexSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
            if (maxTexSize <= 0) {
                return Component.literal("YSM: OpenGL context not available");
            }
            // 原始C++碼檢查了GL20（著色器）和 GL30（VAO）的可用性
            try {
                int testShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
                if (testShader != 0) {
                    GL20.glDeleteShader(testShader);
                }
            } catch (Exception e) {
                return Component.literal("YSM: GL20 (shaders) not available");
            }

            // 预載入default模型，延遲至第一次渲染tick
            // 不能在FMLClientSetupEvent中同步執行ModelAssembler，會導致StackOverflow
            //ClientModelManager.schedulePreloadDefaultModel();
            return null; // 成功
        } catch (Exception e) {
            return Component.literal("YSM Client Init Failed: " + e.getMessage());
        }
    }

    private static void checkNativeInitialization() {
        Component component = (Component) nativeClientInit();
        if (component != null) {
            throw new RuntimeException("YSM Client Initialization Failed: " + component.getString(256));
        }
    }

    // 這裡本來有一個native方法，可能是運行時會初始化載入模型
}
