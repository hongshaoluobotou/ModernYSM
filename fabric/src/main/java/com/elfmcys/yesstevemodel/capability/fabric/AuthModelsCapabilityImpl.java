package com.elfmcys.yesstevemodel.capability.fabric;

import com.elfmcys.yesstevemodel.capability.AuthModelsCapability;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class AuthModelsCapabilityImpl {

    private AuthModelsCapabilityImpl() {
    }

    public static Optional<AuthModelsCapability> get(Player player) {
        AuthModelsComponent component = YsmAttachments.getNullable(player, YsmAttachments.AUTH_MODELS);
        return component == null ? Optional.empty() : Optional.of(component.capability());
    }
}
