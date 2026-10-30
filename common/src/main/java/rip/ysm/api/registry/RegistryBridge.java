package rip.ysm.api.registry;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * 注册表写入的加载器无关入口。
 *
 * <p>为什么需要这层抽象：Fabric 允许在入口点直接 {@code Registry.register(BuiltInRegistries.X, ...)}，
 * 而 NeoForge/Forge 在 mod 构造之前就已冻结内建注册表，必须在 mod 总线的 {@code RegisterEvent} 内注册。
 * 各加载器模块提供同签名的 {@code rip.ysm.api.impl.RegistryImpl}。
 */
public final class RegistryBridge {

    private RegistryBridge() {
    }

    /**
     * 注册一个音效事件。
     *
     * @return 传入的 {@code event}（便于链式赋值；实际绑定由加载器决定，NeoForge 可能延迟到 RegisterEvent）
     */
    public static SoundEvent registerSoundEvent(Identifier id, SoundEvent event) {
        return rip.ysm.api.impl.RegistryImpl.registerSoundEvent(id, event);
    }
}
