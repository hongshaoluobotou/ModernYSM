package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import rip.ysm.api.event.ServerEvents;

public final class ServerStartupEvent {

    private ServerStartupEvent() {
    }

    public static void register() {
        // 原 Architectury LifecycleEvent.SERVER_BEFORE_START 对应「服务器启动中」（Servux 前）
        ServerEvents.onStarting(server -> {
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
