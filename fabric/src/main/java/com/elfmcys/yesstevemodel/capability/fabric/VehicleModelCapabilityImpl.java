package com.elfmcys.yesstevemodel.capability.fabric;

import com.elfmcys.yesstevemodel.capability.VehicleModelCapability;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class VehicleModelCapabilityImpl {

    private VehicleModelCapabilityImpl() {
    }

    public static Optional<VehicleModelCapability> get(Entity entity) {
        VehicleModelComponent component = YsmAttachments.getNullable(entity, YsmAttachments.VEHICLE_MODEL);
        return component == null ? Optional.empty() : Optional.of(component.capability());
    }
}
