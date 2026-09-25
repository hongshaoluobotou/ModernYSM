package com.elfmcys.yesstevemodel.config;

import com.elfmcys.yesstevemodel.YesSteveModel;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class ModSoundEvents {

    public static final SoundEvent CUSTOM_SOUND = SoundEvent.createFixedRangeEvent(Identifier.fromNamespaceAndPath(YesSteveModel.MOD_ID, "custom"), 16.0f);

    public static void register() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, Identifier.fromNamespaceAndPath(YesSteveModel.MOD_ID, "custom"), CUSTOM_SOUND);
    }
}
