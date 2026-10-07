package com.elfmcys.yesstevemodel.fabric.client;

import com.elfmcys.yesstevemodel.capability.fabric.client.PlayerCapabilityClientStore;
import com.elfmcys.yesstevemodel.capability.fabric.client.ProjectileCapabilityClientStore;
import com.elfmcys.yesstevemodel.capability.fabric.client.VehicleCapabilityClientStore;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/** 客户端实体状态的生命周期统一在此接入，避免静态缓存保留已卸载实体和旧世界。 */
public final class ClientStateLifecycle {
    private ClientStateLifecycle() {
    }

    public static void register() {
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            PlayerCapabilityClientStore.remove(entity);
            ProjectileCapabilityClientStore.remove(entity);
            VehicleCapabilityClientStore.remove(entity);
        });
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> {
            // 该事件发生在切换之后；若新世界已有状态，保留它，避免丢掉刚收到的同步。
            PlayerCapabilityClientStore.retainLevel(level);
            ProjectileCapabilityClientStore.retainLevel(level);
            VehicleCapabilityClientStore.retainLevel(level);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> clear());
    }

    private static void clear() {
        PlayerCapabilityClientStore.clear();
        ProjectileCapabilityClientStore.clear();
        VehicleCapabilityClientStore.clear();
    }
}
