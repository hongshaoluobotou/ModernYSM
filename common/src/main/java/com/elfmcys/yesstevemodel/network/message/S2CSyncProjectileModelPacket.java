package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.ProjectileModelCapability;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.network.ClientPayloadSink;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

public class S2CSyncProjectileModelPacket {

    private final int entityId;

    private final ProjectileModelCapability capability;

    private final Int2FloatOpenHashMap floatMap;

    public S2CSyncProjectileModelPacket(int entityId, ProjectileModelCapability capability, Int2FloatOpenHashMap floatMap) {
        this.entityId = entityId;
        this.capability = capability;
        this.floatMap = floatMap;
    }

    public S2CSyncProjectileModelPacket(int entityId, ProjectileModelCapability capability) {
        this(entityId, capability, new Int2FloatOpenHashMap());
    }

    public static void encode(S2CSyncProjectileModelPacket message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.entityId);
        buf.writeNbt(message.capability.serializeNBT());
    }

    public static S2CSyncProjectileModelPacket decode(FriendlyByteBuf buf) {
        int varInt = buf.readVarInt();
        CompoundTag nbt = buf.readNbt();
        ProjectileModelCapability cap = new ProjectileModelCapability();
        if (nbt != null) {
            cap.deserializeNBT(nbt);
        }
        Object2FloatOpenHashMap<String> objectMap = cap.getMolangVars();
        Int2FloatOpenHashMap floatMap = new Int2FloatOpenHashMap();
        objectMap.object2FloatEntrySet().fastForEach(entry -> floatMap.put(StringPool.computeIfAbsent(entry.getKey()), entry.getFloatValue()));
        return new S2CSyncProjectileModelPacket(varInt, cap, floatMap);
    }

    public int entityId() {
        return entityId;
    }

    public ProjectileModelCapability capability() {
        return capability;
    }

    public Int2FloatOpenHashMap floatMap() {
        return floatMap;
    }

    /** 客户端处理器委托，实现见 {@code client.net.ClientPayloadSinkImpl}。 */
    public static void handle(S2CSyncProjectileModelPacket message, PacketContext ctx) {
        ClientPayloadSink.Impl sink = ClientPayloadSink.get();
        if (sink != null) {
            sink.projectileModel(message, ctx);
        }
    }
}