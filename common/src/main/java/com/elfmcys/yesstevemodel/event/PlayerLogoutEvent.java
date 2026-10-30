package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import rip.ysm.api.event.ServerEvents;

public final class PlayerLogoutEvent {

    private PlayerLogoutEvent() {
    }

    public static void register() {
        ServerEvents.onPlayerDisconnect(player -> {
            NetworkHandler.clearClientModelSyncFragments(player.getUUID());
            if (!YesSteveModel.isAvailable()) {
                return;
            }
            ServerModelManager.syncModelToPlayer(player.getUUID());
        });
    }
}
