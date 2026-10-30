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
        // 26.3 port: fabric-api 没有客户端玩家重生事件，本方法由
        // {@link com.elfmcys.yesstevemodel.mixin.client.ClientPacketListenerRespawnMixin}
        // 在 ClientPacketListener#handleRespawn 的 RETURN 处调用（语义 = 1.20.1 Architectury
        // CLIENT_PLAYER_RESPAWN）。保留空实现以维持 YsmEventBootstrap 的注册调用点不变。
    }

    /**
     * 客户端本地玩家重生（旧 LocalPlayer → 新 LocalPlayer）。
     *
     * <p>由 {@code ClientPacketListenerRespawnMixin} 调用，勿直接调用。</p>
     */
    public static void onClientPlayerRespawn(LocalPlayer oldPlayer, LocalPlayer newPlayer) {
        if (!YesSteveModel.isAvailable() || !NetworkHandler.isClientConnected()) {
            return;
        }
        CapabilityLifecycle.revive(oldPlayer);
        PlayerCapability.get(oldPlayer).ifPresent(cap -> PlayerCapability.get(newPlayer).ifPresent(cap2 -> cap2.copyFrom(cap)));
        CapabilityLifecycle.invalidate(oldPlayer);
    }
}