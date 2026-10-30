package rip.ysm.api.impl;

import com.elfmcys.yesstevemodel.forge.ForgeModBus;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge 侧的 {@link rip.ysm.api.registry.RegistryBridge} 实现。
 *
 * <p>Forge 与 NeoForge 一样在 mod 构造前冻结内建注册表，必须在 {@code RegisterEvent} 内注册；
 * 该事件挂在**mod BusGroup** 上（{@code RegisterEvent.getBus(modBusGroup)}），
 * 需由入口点先把 {@code FMLJavaModLoadingContext#getModBusGroup()} 存进 {@link ForgeModBus}。
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
        // 先取总线再置 hooked：ForgeModBus 未就绪时 get() 会抛 IllegalStateException，
        // 若先置位就会"第一次抛异常、之后静默不再挂钩"→ 内建音效永远注册不上（模型包全程静音）。
        var bus = RegisterEvent.getBus(ForgeModBus.get());
        hooked = true;
        bus.addListener(registerEvent -> {
            if (!Registries.SOUND_EVENT.equals(registerEvent.getRegistryKey())) {
                return;
            }
            for (Pending pending : PENDING) {
                registerEvent.register(Registries.SOUND_EVENT, pending.id(), () -> pending.event());
            }
            PENDING.clear();
        });
    }
}
