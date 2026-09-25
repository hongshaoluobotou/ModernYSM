package com.elfmcys.yesstevemodel.capability.fabric;

import com.elfmcys.yesstevemodel.capability.StarModelsCapability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class StarModelsComponent implements YsmComponent {

    private final StarModelsCapability capability = new StarModelsCapability();

    public StarModelsCapability capability() {
        return capability;
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        ListTag list = tag.getListOrEmpty("StarModels");
        capability.deserializeNBT(list);
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.put("StarModels", capability.serializeNBT());
    }
}
