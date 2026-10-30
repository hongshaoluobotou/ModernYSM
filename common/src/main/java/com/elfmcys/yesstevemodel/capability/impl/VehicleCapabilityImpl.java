package com.elfmcys.yesstevemodel.capability.impl;

import com.elfmcys.yesstevemodel.capability.VehicleCapability;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import com.elfmcys.yesstevemodel.capability.impl.client.VehicleCapabilityClientStore;

public final class VehicleCapabilityImpl {

    private VehicleCapabilityImpl() {
    }

    public static Optional<VehicleCapability> get(Entity entity) {
        if (rip.ysm.api.PlatformAPI.isServer() || !entity.level().isClientSide()) {
            return Optional.empty();
        }
        return VehicleCapabilityClientStore.get(entity);
    }
}
