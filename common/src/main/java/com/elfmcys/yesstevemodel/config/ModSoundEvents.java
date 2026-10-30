package com.elfmcys.yesstevemodel.config;

import com.elfmcys.yesstevemodel.YesSteveModel;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import rip.ysm.api.registry.RegistryBridge;

public class ModSoundEvents {

    public static final SoundEvent CUSTOM_SOUND = SoundEvent.createFixedRangeEvent(Identifier.fromNamespaceAndPath(YesSteveModel.MOD_ID, "custom"), 16.0f);

    public static void register() {
        // 经加载器抽象注册：Fabric 直接写入内建注册表，NeoForge/Forge 延迟到 RegisterEvent
        RegistryBridge.registerSoundEvent(
                Identifier.fromNamespaceAndPath(YesSteveModel.MOD_ID, "custom"), CUSTOM_SOUND);
    }
}
