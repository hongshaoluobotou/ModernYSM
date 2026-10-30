package com.elfmcys.yesstevemodel.capability.impl;

import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import com.elfmcys.yesstevemodel.capability.impl.client.PlayerCapabilityClientStore;

public final class PlayerCapabilityImpl {

    private PlayerCapabilityImpl() {
    }

    public static Optional<PlayerCapability> get(Player player) {
        if (rip.ysm.api.PlatformAPI.isServer()) {
            return Optional.empty();
        }
        return PlayerCapabilityClientStore.get(player);
    }

    public static Optional<PlayerCapability> get(Entity entity) {
        if (!(entity instanceof Player player)) {
            return Optional.empty();
        }
        return get(player);
    }
}
