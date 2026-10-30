package rip.ysm.api.impl;

import com.elfmcys.yesstevemodel.neoforge.NeoForgeModBus;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * NeoForge 侧的 {@link rip.ysm.api.registry.RegistryBridge} 实现。
 *
 * <p>NeoForge 在 mod 构造之前就冻结了内建注册表（直接 {@code Registry.register} 会抛
 * "Registry is already frozen"），必须在 mod 总线的 {@code RegisterEvent} 内注册。
 * common 侧在 mod 构造期就会调用本类，故先入队，事件触发时统一落地。
 */
public final class RegistryImpl {

    private record Pending(Identifier id, SoundEvent event) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();
    private static boolean hooked;

    private RegistryImpl() {
    }

    public static SoundEvent registerSoundEvent(Identifier id, SoundEvent event) {
        ensureHooked();
        PENDING.add(new Pending(id, event));
        return event;
    }

    private static void ensureHooked() {
        if (hooked) {
            return;
        }
        hooked = true;
        NeoForgeModBus.get().addListener(RegisterEvent.class, event -> {
            if (!Registries.SOUND_EVENT.equals(event.getRegistryKey())) {
                return;
            }
            for (Pending pending : PENDING) {
                event.register(Registries.SOUND_EVENT, pending.id(), () -> pending.event());
            }
            PENDING.clear();
        });
    }
}
