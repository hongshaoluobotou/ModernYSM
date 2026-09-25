package com.elfmcys.yesstevemodel.mixin;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.fabric.YsmAttachments;
import com.elfmcys.yesstevemodel.event.CapabilityEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 恢复玩家 respawn 数据复制（原 Forge PLAYER_CLONE / CCA RespawnCopyStrategy.ALWAYS_COPY）：
 * vanilla 把旧玩家数据复制给新玩家实例后，把附加数据无条件复制过去，并回调 CapabilityEvent.onPlayerCloned。
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Inject(method = "respawn(Lnet/minecraft/server/level/ServerPlayer;ZLnet/minecraft/world/entity/Entity$RemovalReason;)Lnet/minecraft/server/level/ServerPlayer;", at = @At("RETURN"))
    private void ysm$onRespawn(ServerPlayer player, boolean alive, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        ServerPlayer newPlayer = cir.getReturnValue();
        if (newPlayer == null || newPlayer == player) {
            return;
        }
        YsmAttachments.copyAll(player, newPlayer);
        CapabilityEvent.onPlayerCloned(player, newPlayer, !alive);
    }
}
