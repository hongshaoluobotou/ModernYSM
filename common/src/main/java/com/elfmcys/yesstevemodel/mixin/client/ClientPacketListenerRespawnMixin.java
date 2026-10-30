package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.client.event.ClientPlayerCloneEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端玩家重生桥（1.20.1 Architectury {@code ClientPlayerEvent.CLIENT_PLAYER_RESPAWN} 的 26.3 等价物）。
 *
 * <p>26.3 没有现成的客户端重生事件（fabric-api 只到 {@code ClientPlayConnectionEvents}），
 * 但 vanilla 的 {@code ClientPacketListener#handleRespawn(ClientboundRespawnPacket)}
 * 内部就是「取出旧 LocalPlayer → 新建 LocalPlayer → 赋给 {@code minecraft.player}」：
 * HEAD 时 {@code minecraft.player} 仍是旧玩家，RETURN 时已是新玩家，
 * 与 Architectury 事件的 (oldPlayer, newPlayer) 语义逐项一致。</p>
 *
 * <p>服务端侧的能力复制已由 {@code PlayerListMixin}（{@code PlayerList#respawn}）完成，
 * 本 mixin 只负责客户端本地玩家的能力对象搬运（不跨维度时服务端不会重发完整同步包，
 * 客户端必须自己把旧 LocalPlayer 的 {@link com.elfmcys.yesstevemodel.capability.PlayerCapability}
 * 复制到新实例，否则选中的模型会"重生后变回史蒂夫"）。</p>
 *
 * <p>三个加载器的 {@code handleRespawn} 签名一致（已用 javap 核对 fabric merged jar /
 * NeoForge patched jar / Forge injected.jar），故放在 common 的 mixins.json。</p>
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerRespawnMixin {

    @Unique
    private LocalPlayer ysm$oldPlayer;

    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void ysm$captureOldPlayer(ClientboundRespawnPacket packet, CallbackInfo ci) {
        this.ysm$oldPlayer = Minecraft.getInstance().player;
    }

    @Inject(method = "handleRespawn", at = @At("RETURN"))
    private void ysm$onRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        LocalPlayer oldPlayer = this.ysm$oldPlayer;
        this.ysm$oldPlayer = null;
        LocalPlayer newPlayer = Minecraft.getInstance().player;
        if (oldPlayer != null && newPlayer != null && oldPlayer != newPlayer) {
            ClientPlayerCloneEvent.onClientPlayerRespawn(oldPlayer, newPlayer);
        }
    }
}
