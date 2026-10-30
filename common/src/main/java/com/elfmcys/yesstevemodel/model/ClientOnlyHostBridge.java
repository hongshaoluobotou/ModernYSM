package com.elfmcys.yesstevemodel.model;

import com.elfmcys.yesstevemodel.client.ClientOnlyMode;
import rip.ysm.api.env.EnvType;
import rip.ysm.api.env.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.UUID;

@Environment(EnvType.CLIENT)
final class ClientOnlyHostBridge {

    private ClientOnlyHostBridge() {
    }

    static boolean isActive() {
        return ClientOnlyMode.isActive();
    }

    static boolean isLocalHost(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getUser() != null && uuid.equals(minecraft.getUser().getProfileId())) {
            return true;
        }
        LocalPlayer player = minecraft.player;
        return player != null && uuid.equals(player.getUUID());
    }
}
