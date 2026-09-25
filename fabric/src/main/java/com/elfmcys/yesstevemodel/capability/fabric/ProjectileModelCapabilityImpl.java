package com.elfmcys.yesstevemodel.capability.fabric;

import com.elfmcys.yesstevemodel.capability.ProjectileModelCapability;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.Optional;

public final class ProjectileModelCapabilityImpl {

    private ProjectileModelCapabilityImpl() {
    }

    public static Optional<ProjectileModelCapability> get(Entity entity) {
        if (!(entity instanceof Projectile)) {
            return Optional.empty();
        }
        ProjectileModelComponent component = YsmAttachments.getNullable(entity, YsmAttachments.PROJECTILE_MODEL);
        return component == null ? Optional.empty() : Optional.of(component.capability());
    }

    public static Optional<ProjectileModelCapability> get(Projectile projectile) {
        ProjectileModelComponent component = YsmAttachments.getNullable(projectile, YsmAttachments.PROJECTILE_MODEL);
        return component == null ? Optional.empty() : Optional.of(component.capability());
    }
}
