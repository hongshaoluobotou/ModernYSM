package com.elfmcys.yesstevemodel.capability.fabric.client;

import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.entity.PlayerPreviewEntity;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PlayerCapabilityClientStore {

    private static final ConcurrentMap<UUID, PlayerCapability> STORE = new ConcurrentHashMap<>();

    private PlayerCapabilityClientStore() {
    }

    public static Optional<PlayerCapability> get(Player player) {
        if (!(player instanceof AbstractClientPlayer)) {
            return Optional.empty();
        }
        // 26.3 port: GUI 预览实体（DummyPlayer）的模型加载在 PlayerPreviewEntity 包装器上，
        // 不新建空 Capability（否则 PiP 预览会回退原版皮肤），直接返回包装器。
        PlayerPreviewEntity preview = PlayerPreviewEntity.getWrapper(player);
        if (preview != null) {
            return Optional.of(preview);
        }
        UUID uuid = player.getUUID();
        PlayerCapability existing = STORE.get(uuid);
        if (existing != null && existing.entity == player) {
            return Optional.of(existing);
        }
        PlayerCapability fresh = new PlayerCapability(player);
        STORE.put(uuid, fresh);
        return Optional.of(fresh);
    }

    public static void clear() {
        STORE.clear();
    }
}
