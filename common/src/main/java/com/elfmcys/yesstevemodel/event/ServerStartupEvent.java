package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class ServerStartupEvent {

    private ServerStartupEvent() {
    }

    public static void register() {
        // 原 Architectury LifecycleEvent.SERVER_BEFORE_START 对应 Fabric 的 SERVER_STARTING
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (!YesSteveModel.isAvailable()) {
                return;
            }
            if (ServerModelManager.canReuseLoadedModels()) {
                return;
            }
            ServerModelManager.loadModels(result -> {
                if (!result.isSuccess()) {
                    server.execute(() -> {
                        throw new RuntimeException("YSM Loading Failed: " + result.getErrorMessage().getString(256));
                    });
                }
            }, null);
        });
    }
}
