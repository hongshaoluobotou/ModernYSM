package rip.ysm.api.impl;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge 侧的按键绑定注册（{@code RegisterKeyMappingsEvent}）。
 *
 * <p>与 NeoForge 版一致：{@link KeyMapping} 对象可以提前构造（调用方要持有实例查询状态），
 * 只有「注册」这一动作必须发生在事件内，故先入队、事件触发时统一注册。
 */
public final class KeyMappingRegistryImpl {

    private static final List<KeyMapping> PENDING = new ArrayList<>();
    private static boolean hooked;

    private KeyMappingRegistryImpl() {
    }

    public static void init() {
        if (hooked) {
            return;
        }
        hooked = true;
        RegisterKeyMappingsEvent.BUS.addListener(event -> {
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
