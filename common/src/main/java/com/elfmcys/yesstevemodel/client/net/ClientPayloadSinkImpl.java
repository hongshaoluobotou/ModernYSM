package com.elfmcys.yesstevemodel.client.net;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.AuthModelsCapability;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.capability.ProjectileCapability;
import com.elfmcys.yesstevemodel.capability.StarModelsCapability;
import com.elfmcys.yesstevemodel.capability.VehicleCapability;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.ClientOnlyMode;
import com.elfmcys.yesstevemodel.client.upload.ModelUploadSession;
import com.elfmcys.yesstevemodel.event.EntityJoinCallbackEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.geckolib3.resource.GeckoLibCache;
import com.elfmcys.yesstevemodel.molang.parser.ParseException;
import com.elfmcys.yesstevemodel.network.ClientPayloadSink;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.S2CExecuteMolangPacket;
import com.elfmcys.yesstevemodel.network.message.S2CModelSyncPayload;
import com.elfmcys.yesstevemodel.network.message.S2CModelUploadResultPacket;
import com.elfmcys.yesstevemodel.network.message.S2CModelUploadStartPacket;
import com.elfmcys.yesstevemodel.network.message.S2CSetModelAndTexturePacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncAnimationExpressionPacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncAuthModelsPacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncPlayerStatePacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncProjectileModelPacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncStarModelsPacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncVehicleModelPacket;
import com.elfmcys.yesstevemodel.network.message.S2CVersionCheckPacket;
import com.elfmcys.yesstevemodel.capability.ProjectileModelCapability;
import com.elfmcys.yesstevemodel.capability.VehicleModelCapability;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.StringUtils;
import rip.ysm.api.network.PacketContext;
import rip.ysm.compat.touhoulittlemaid.TouhouMaidCompat;

/**
 * 客户端接收处理器实现（{@link ClientPayloadSink.Impl}）。
 *
 * <p>本类引用 {@code net.minecraft.client.*}，**只能**在物理客户端被加载与安装
 * （由各加载器的客户端入口点 {@code ClientPayloadSink.install(new ClientPayloadSinkImpl())}）。
 * 专用服务器上这些处理器全部为空转——S2C 包本来也不会到达服务端。
 */
public final class ClientPayloadSinkImpl implements ClientPayloadSink.Impl {

    @Override
    public void executeMolang(S2CExecuteMolangPacket message, PacketContext ctx) {
        if (!ctx.isClientSide()) {
            return;
        }
        ctx.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return;
            }
            for (int i : message.entityIds()) {
                Entity entity = minecraft.level.getEntity(i);
                if (entity instanceof Player) {
                    PlayerCapability.get(entity).ifPresent(cap -> {
                        try {
                            cap.executeExpression(GeckoLibCache.parseSimpleExpression(message.expression()), true, false, null);
                        } catch (ParseException e) {
                            YesSteveModel.LOGGER.error("Failed to execute molang " + message.expression(), e);
                        }
                    });
                } else if (TouhouMaidCompat.isMaidEntity(entity)) {
                    TouhouMaidCompat.playMaidAnimation(entity, message.expression());
                }
            }
        });
    }

    @Override
    public void modelSync(S2CModelSyncPayload message, PacketContext ctx) {
        if (ctx.isClientSide()) {
            ctx.enqueueWork(() -> ClientModelManager.startSync(ctx.getConnection(), message.data()));
        }
    }

    @Override
    public void modelUploadStart(S2CModelUploadStartPacket packet, PacketContext ctx) {
        if (ctx.isClientSide()) {
            ctx.enqueueWork(() -> ModelUploadSession.onStartAck(packet.uploadId(), packet.status(), packet.chunkSize(),
                    packet.maxTotalBytes(), packet.chunksPerTick(), packet.message()));
        }
    }

    @Override
    public void modelUploadResult(S2CModelUploadResultPacket packet, PacketContext ctx) {
        if (ctx.isClientSide()) {
            ctx.enqueueWork(() -> ModelUploadSession.onResult(packet.uploadId(), packet.status(), packet.modelId(),
                    packet.h1(), packet.h2(), packet.message()));
        }
    }

    @Override
    public void setModelAndTexture(S2CSetModelAndTexturePacket message, PacketContext ctx) {
        if (ctx.isClientSide()) {
            EntityJoinCallbackEvent.addCallback(message.entityId(), entity -> applyModelAndTexture(entity, message));
        }
    }

    @Override
    public void animationExpression(S2CSyncAnimationExpressionPacket message, PacketContext ctx) {
        if (ctx.isClientSide()) {
            ctx.enqueueWork(() -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.level == null) {
                    return;
                }
                PlayerCapability.get(minecraft.level.getEntity(message.entityId()))
                        .ifPresent(cap -> cap.executeAnimationExpression(message.floatData()));
            });
        }
    }

    @Override
    public void authModels(S2CSyncAuthModelsPacket message, PacketContext ctx) {
        if (ctx.isClientSide()) {
            ctx.enqueueWork(() -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player != null) {
                    AuthModelsCapability.get(minecraft.player).ifPresent(cap -> cap.setAuthModels(message.authModels()));
                }
            });
        }
    }

    @Override
    public void playerState(S2CSyncPlayerStatePacket message, PacketContext ctx) {
        if (ctx.isClientSide()) {
            EntityJoinCallbackEvent.addCallback(message.entityId, entity -> applyPlayerState(entity, message));
        }
    }

    @Override
    public void projectileModel(S2CSyncProjectileModelPacket message, PacketContext ctx) {
        if (ctx.isClientSide()) {
            EntityJoinCallbackEvent.addCallback(message.entityId(),
                    entity -> applyProjectileModel(entity, message.capability(), message.floatMap()));
        }
    }

    @Override
    public void starModels(S2CSyncStarModelsPacket message, PacketContext ctx) {
        if (ctx.isClientSide()) {
            ctx.enqueueWork(() -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player != null) {
                    StarModelsCapability.get(minecraft.player).ifPresent(cap -> cap.setStarModels(message.starModels()));
                }
            });
        }
    }

    @Override
    public void vehicleModel(S2CSyncVehicleModelPacket message, PacketContext ctx) {
        if (ctx.isClientSide()) {
            EntityJoinCallbackEvent.addCallback(message.entityId(),
                    entity -> applyVehicleModel(entity, message.capability(), message.floatMap()));
        }
    }

    @Override
    public void versionCheck(S2CVersionCheckPacket message, PacketContext ctx) {
        NetworkHandler.setServerSupportsModelSyncFragments(message.supportsModelSyncFragments());
        if (NetworkHandler.setChannelVersion(ctx.getConnection(), message.version())) {
            ctx.enqueueWork(() -> {
                ClientOnlyMode.leaveStandalone();
                ClientModelManager.onSyncConnected();
            });
        }
        if (NetworkHandler.VERSION.equals(message.version())) {
            NetworkHandler.markClientHandshakeComplete();
        }
        NetworkHandler.sendVersionCheck(ctx.getConnection());
    }

    @Override
    public void oysmBrand(boolean allowUpload) {
        ClientModelManager.setOysmServer(true);
        ClientModelManager.setAllowUpload(allowUpload);
    }

    // ---------------- 供包内回调 / 其他客户端代码复用的静态处理器 ----------------

    public static void applyModelAndTexture(Entity entity, S2CSetModelAndTexturePacket other) {
        boolean keepLocalModel = ClientOnlyMode.isForced() && entity == Minecraft.getInstance().player;
        PlayerCapability.get(entity).ifPresent(cap -> {
            if (!keepLocalModel) {
                cap.initModelWithTexture(other.modelId(), other.textureId());
                cap.setForceDisabled(other.disabled());
            }
            applyPlayerState(entity, other.entityModelSync());
        });
    }

    public static void applyPlayerState(Entity entity, S2CSyncPlayerStatePacket message) {
        if (entity instanceof Player) {
            PlayerCapability.get(entity).ifPresent(cap -> {
                if ((message.flags & 2048) != 0) {
                    if (!StringUtils.isEmpty(message.modelSwitchId)) {
                        cap.requestModelSwitch(message.modelSwitchId);
                    } else {
                        cap.clearModelSwitch();
                    }
                }
                if ((message.flags & 4096) != 0) {
                    if (message.isFullSync()) {
                        cap.updateMolangVars(message.molangHashId(), (Int2FloatOpenHashMap) message.molangVarData);
                    } else {
                        cap.enqueueMolangDelta(message.molangHashId(), message.molangVarData);
                    }
                }
                cap.getPositionTracker().applySyncMessage(message);
            });
        }
    }

    public static void applyProjectileModel(Entity entity, ProjectileModelCapability capability, Int2FloatOpenHashMap floatMap) {
        ProjectileCapability.get(entity).ifPresent(projectileCapability -> {
            projectileCapability.updateModelId(capability.getOwnerModelId());
            projectileCapability.setFloatProperties(floatMap);
        });
    }

    public static void applyVehicleModel(Entity entity, VehicleModelCapability capability, Int2FloatOpenHashMap floatMap) {
        VehicleCapability.get(entity).ifPresent(vehicleCapability -> {
            vehicleCapability.setOwnerModelId(capability.getOwnerModelId());
            vehicleCapability.setFloatMap(floatMap);
        });
    }
}
