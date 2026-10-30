package com.elfmcys.yesstevemodel.neoforge;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.ProjectileModelCapability;
import com.elfmcys.yesstevemodel.capability.VehicleModelCapability;
import com.elfmcys.yesstevemodel.event.CapabilityEvent;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.S2CSyncProjectileModelPacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncVehicleModelPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * NeoForge 侧的实体追踪同步（fabric 的 {@code EntityTrackingEvents.START_TRACKING} 等价物）。
 */
public final class NeoForgeCapabilityHooks {

    private NeoForgeCapabilityHooks() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PlayerEvent.StartTracking.class, event -> {
            if (event.getEntity() instanceof ServerPlayer viewer) {
                onStartTracking(event.getTarget(), viewer);
            }
        });
    }

    private static void onStartTracking(Entity target, ServerPlayer viewer) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        if (target instanceof ServerPlayer trackedPlayer) {
            CapabilityEvent.getModelInfoCap(trackedPlayer).ifPresent(cap -> {
                if (!NetworkHandler.isPlayerConnected(trackedPlayer) && !cap.isMandatory()) {
                    return;
                }
                cap.createSyncMessage(trackedPlayer, false)
                        .ifPresentOrElse(
                                message -> NetworkHandler.sendToClientPlayer(message, viewer),
                                cap::markDirty
                        );
            });
            return;
        }
        if (target instanceof Projectile projectile) {
            ProjectileModelCapability.get(projectile).ifPresent(cap -> {
                if (cap.isInitialized()) {
                    NetworkHandler.sendToClientPlayer(
                            new S2CSyncProjectileModelPacket(projectile.getId(), cap),
                            viewer
                    );
                }
            });
            return;
        }
        VehicleModelCapability.get(target).ifPresent(cap -> {
            if (cap.isInitialized()) {
                NetworkHandler.sendToClientPlayer(
                        new S2CSyncVehicleModelPacket(target.getId(), cap),
                        viewer
                );
            }
        });
    }
}
