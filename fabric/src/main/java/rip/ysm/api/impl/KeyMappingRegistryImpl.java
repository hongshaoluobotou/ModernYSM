package rip.ysm.api.impl;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/**
 * Fabric 侧的按键绑定注册（{@code KeyMappingHelper.registerKeyMapping}）。
 */
public final class KeyMappingRegistryImpl {

    private KeyMappingRegistryImpl() {
    }

    public static KeyMapping registerKeyMapping(KeyMapping mapping) {
        return KeyMappingHelper.registerKeyMapping(mapping);
    }
}
