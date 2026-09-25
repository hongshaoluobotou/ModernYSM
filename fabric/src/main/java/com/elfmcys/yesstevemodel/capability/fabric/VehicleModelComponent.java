package com.elfmcys.yesstevemodel.capability.fabric;

import com.elfmcys.yesstevemodel.capability.VehicleModelCapability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class VehicleModelComponent implements YsmComponent {

    private final VehicleModelCapability capability = new VehicleModelCapability();

    public VehicleModelCapability capability() {
        return capability;
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        if (tag.contains("VehicleModel")) {
            capability.deserializeNBT(tag.getCompoundOrEmpty("VehicleModel"));
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.put("VehicleModel", capability.serializeNBT());
    }
}
