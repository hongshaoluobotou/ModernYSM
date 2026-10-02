package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.animation.AnimationRegister;
import com.elfmcys.yesstevemodel.client.input.AnimationRouletteKey;
import com.elfmcys.yesstevemodel.client.input.ExtraAnimationKey;
import com.elfmcys.yesstevemodel.client.input.ExtraPlayerRenderKey;
import com.elfmcys.yesstevemodel.client.input.PlayerModelToggleKey;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
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
        // 渲染管线原生适配（阶段①）：原 GL 上下文自检（nativeClientInit：GL_MAX_TEXTURE_SIZE/GL20 shader
        // 探测）服务于 ysm-core natives 初始化，native 路径已删除；且在 Vulkan 后端挡位下直接探测 GL11/GL20
        // 语义不成立，一并移除（同时消除一处裸 org.lwjgl.opengl 依赖）。
        AnimationRegister.registerAnimationState();
    }

    private static void registerKeyMappings() {
        registerKeyMappingIfPresent("com.elfmcys.yesstevemodel.client.input.PlayerModelToggleKey", "KEY_MAPPING");
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        registerKeyMappingIfPresent("com.elfmcys.yesstevemodel.client.input.AnimationRouletteKey", "KEY_ROULETTE");
        registerKeyMappingIfPresent("com.elfmcys.yesstevemodel.client.input.AnimationRouletteKey", "KEY_LOCK");
        registerKeyMappingIfPresent("com.elfmcys.yesstevemodel.client.input.DebugAnimationKey", "KEY_MAPPING");
        registerKeyMappingIfPresent("com.elfmcys.yesstevemodel.client.input.ExtraPlayerRenderKey", "KEY_MAPPING");
        for (KeyMapping mapping : ExtraAnimationKey.getKeyMappings()) {
            KeyMappingHelper.registerKeyMapping(mapping);
        }
    }

    private static void registerKeyMappingIfPresent(String className, String fieldName) {
        try {
            KeyMappingHelper.registerKeyMapping((KeyMapping) Class.forName(className).getField(fieldName).get(null));
        } catch (Throwable t) {
            YesSteveModel.LOGGER.error("Failed to register key mapping {}.{}", className, fieldName, t);
        }
    }
}
