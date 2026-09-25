package com.elfmcys.yesstevemodel.capability.fabric;

import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class ModelInfoComponent implements YsmComponent {

    private final ModelInfoCapability capability = new ModelInfoCapability();

    public ModelInfoCapability capability() {
        return capability;
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        if (tag.contains("ModelInfo")) {
            capability.deserializeNBT(tag.getCompoundOrEmpty("ModelInfo"));
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.put("ModelInfo", capability.serializeNBT());
    }
}
