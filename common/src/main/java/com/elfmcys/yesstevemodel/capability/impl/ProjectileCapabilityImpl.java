package com.elfmcys.yesstevemodel.capability.impl;

import com.elfmcys.yesstevemodel.capability.ProjectileCapability;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.Optional;
import com.elfmcys.yesstevemodel.capability.impl.client.ProjectileCapabilityClientStore;

public final class ProjectileCapabilityImpl {

    private ProjectileCapabilityImpl() {
    }

    public static Optional<ProjectileCapability> get(Entity entity) {
        if (!(entity instanceof Projectile projectile)) {
            return Optional.empty();
        }
        return get(projectile);
    }

    public static Optional<ProjectileCapability> get(Projectile projectile) {
        if (rip.ysm.api.PlatformAPI.isServer() || !projectile.level().isClientSide()) {
            return Optional.empty();
        }
        return ProjectileCapabilityClientStore.get(projectile);
    }
}
