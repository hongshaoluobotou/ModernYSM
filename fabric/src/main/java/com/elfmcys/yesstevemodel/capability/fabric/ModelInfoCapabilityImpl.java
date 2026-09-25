package com.elfmcys.yesstevemodel.capability.fabric;

import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class ModelInfoCapabilityImpl {

    private ModelInfoCapabilityImpl() {
    }

    public static Optional<ModelInfoCapability> get(Player player) {
        ModelInfoComponent component = YsmAttachments.getNullable(player, YsmAttachments.MODEL_INFO);
        return component == null ? Optional.empty() : Optional.of(component.capability());
    }
}
