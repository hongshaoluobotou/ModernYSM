package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.client.player.LocalPlayer;
import rip.ysm.api.PlatformAPI;
import rip.ysm.api.capability.CapabilityLifecycle;

public final class ClientPlayerCloneEvent {

    private ClientPlayerCloneEvent() {
    }

    public static void register() {
        // TODO port: Fabric API 没有客户端玩家重生（CLIENT_PLAYER_RESPAWN）事件，
        // onClientPlayerRespawn 需要通过 mixin 到 LocalPlayer / ClientPacketListener 的 respawn 流程恢复注册，当前暂未迁移。
    }

    private static void onClientPlayerRespawn(LocalPlayer oldPlayer, LocalPlayer newPlayer) {
        if (!YesSteveModel.isAvailable() || !NetworkHandler.isClientConnected()) {
            return;
        }
        CapabilityLifecycle.revive(oldPlayer);
        PlayerCapability.get(oldPlayer).ifPresent(cap -> PlayerCapability.get(newPlayer).ifPresent(cap2 -> cap2.copyFrom(cap)));
        CapabilityLifecycle.invalidate(oldPlayer);
    }
}