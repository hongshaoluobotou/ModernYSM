package rip.ysm.api.impl;

import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * NeoForge 侧的按键绑定注册（{@code RegisterKeyMappingsEvent#register}）。
 *
 * <p>{@link KeyMapping} 对象本身可以提前构造（调用方需要持有实例查询按下状态），
 * 只有“注册”这一动作必须发生在 mod 总线事件内，故先入队、事件触发时统一注册。
 */
public final class KeyMappingRegistryImpl {

    private static final List<KeyMapping> PENDING = new ArrayList<>();
    private static boolean hooked;

    private KeyMappingRegistryImpl() {
    }

    public static void init(IEventBus modBus) {
        if (hooked) {
            return;
        }
        hooked = true;
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> {
            for (KeyMapping mapping : PENDING) {
                event.register(mapping);
            }
            PENDING.clear();
        });
    }

    public static KeyMapping registerKeyMapping(KeyMapping mapping) {
        PENDING.add(mapping);
        return mapping;
    }
}
