package com.elfmcys.yesstevemodel.capability.fabric.client;

import com.elfmcys.yesstevemodel.capability.ProjectileCapability;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ProjectileCapabilityClientStore {

    private static final ConcurrentMap<UUID, ProjectileCapability> STORE = new ConcurrentHashMap<>();

    private ProjectileCapabilityClientStore() {
    }

    public static Optional<ProjectileCapability> get(Projectile projectile) {
        return Optional.of(STORE.compute(projectile.getUUID(), (uuid, existing) ->
                existing != null && existing.entity == projectile ? existing : new ProjectileCapability(projectile)));
    }

    public static void remove(Entity entity) {
        STORE.computeIfPresent(entity.getUUID(), (uuid, cap) -> cap.entity == entity ? null : cap);
    }

    public static void retainLevel(Level level) {
        STORE.values().removeIf(cap -> cap.entity.level() != level);
    }

    public static void clear() {
        STORE.clear();
    }
}
