package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import rip.ysm.api.PlatformAPI;
import rip.ysm.compat.touhoulittlemaid.TouhouMaidCompat;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import java.io.IOException;

public final class CommonEvent {

    private CommonEvent() {
    }

    public static Object nativeInit() {
        try {
            ServerModelManager.reloadPacks();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static void register() {
        // TODO port: 原 Architectury LifecycleEvent.SETUP 在模组初始化阶段触发，
        // Fabric-only 后公共 entrypoint 初始化时即为此阶段，这里直接内联执行。
        if (!YesSteveModel.isAvailable()) {
            YesSteveModel.LOGGER.error(YesSteveModel.getErrorMessage());
            return;
        }
        NetworkHandler.init();
        TouhouMaidCompat.init();
        nativeInit();
    }
}