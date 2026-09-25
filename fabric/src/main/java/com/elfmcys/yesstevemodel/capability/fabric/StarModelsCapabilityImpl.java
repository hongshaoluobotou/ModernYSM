package com.elfmcys.yesstevemodel.capability.fabric;

import com.elfmcys.yesstevemodel.capability.StarModelsCapability;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class StarModelsCapabilityImpl {

    private StarModelsCapabilityImpl() {
    }

    public static Optional<StarModelsCapability> get(Player player) {
        StarModelsComponent component = YsmAttachments.getNullable(player, YsmAttachments.STAR_MODELS);
        return component == null ? Optional.empty() : Optional.of(component.capability());
    }
}
