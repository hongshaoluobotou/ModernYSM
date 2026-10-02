package com.elfmcys.yesstevemodel.capability.fabric.client;

import com.elfmcys.yesstevemodel.capability.VehicleCapability;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class VehicleCapabilityClientStore {

    private static final ConcurrentMap<UUID, VehicleCapability> STORE = new ConcurrentHashMap<>();

    private VehicleCapabilityClientStore() {
    }

    public static Optional<VehicleCapability> get(Entity entity) {
        return Optional.of(STORE.compute(entity.getUUID(), (uuid, existing) ->
                existing != null && existing.entity == entity ? existing : new VehicleCapability(entity)));
    }

    public static void remove(Entity entity) {
        // 旧实体的延迟卸载事件不能移除同 UUID 的新实例。
        STORE.computeIfPresent(entity.getUUID(), (uuid, cap) -> cap.entity == entity ? null : cap);
    }

    public static void retainLevel(Level level) {
        STORE.values().removeIf(cap -> cap.entity.level() != level);
    }

    public static void clear() {
        STORE.clear();
    }
}
