package rip.ysm.api.client;

import net.minecraft.client.KeyMapping;

/**
 * 按键绑定注册的加载器无关入口。
 *
 * <p>各加载器对应的注册方式：
 * <ul>
 *   <li>Fabric：{@code KeyMappingHelper.registerKeyMapping(mapping)}</li>
 *   <li>NeoForge/Forge：{@code RegisterKeyMappingsEvent.register(mapping)}</li>
 * </ul>
 *
 * <p>必须在加载器的按键注册钩子内调用；在钩子之外调用会抛异常（防止键位静默丢失）。
 */
public final class KeyMappingRegistry {

    private KeyMappingRegistry() {
    }

    public static KeyMapping register(KeyMapping mapping) {
        return rip.ysm.api.impl.KeyMappingRegistryImpl.registerKeyMapping(mapping);
    }
}
