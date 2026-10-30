package rip.ysm.api.impl;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Fabric 侧的 {@link rip.ysm.api.registry.RegistryBridge} 实现：内建注册表未冻结，直接写入。
 */
public final class RegistryImpl {

    private RegistryImpl() {
    }

    public static SoundEvent registerSoundEvent(Identifier id, SoundEvent event) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, event);
    }
}
