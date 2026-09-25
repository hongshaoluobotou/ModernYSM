package com.elfmcys.yesstevemodel.capability.fabric;

import com.elfmcys.yesstevemodel.capability.ProjectileModelCapability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class ProjectileModelComponent implements YsmComponent {

    private final ProjectileModelCapability capability = new ProjectileModelCapability();

    public ProjectileModelCapability capability() {
        return capability;
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        if (tag.contains("ProjectileModel")) {
            capability.deserializeNBT(tag.getCompoundOrEmpty("ProjectileModel"));
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.put("ProjectileModel", capability.serializeNBT());
    }
}
