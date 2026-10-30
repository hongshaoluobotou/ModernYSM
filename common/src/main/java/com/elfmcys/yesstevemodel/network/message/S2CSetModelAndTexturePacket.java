package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.network.ClientPayloadSink;
import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

public class S2CSetModelAndTexturePacket {

    private final int entityId;

    private final String modelId;

    private final String textureId;

    private final boolean disabled;

    private final S2CSyncPlayerStatePacket entityModelSync;

    public S2CSetModelAndTexturePacket(int entityId, String modelId, String textureId, boolean disabled, S2CSyncPlayerStatePacket playerState) {
        this.entityId = entityId;
        this.modelId = modelId;
        this.textureId = textureId;
        this.entityModelSync = playerState;
        this.disabled = disabled;
    }

    public static void encode(S2CSetModelAndTexturePacket other, FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeVarInt(other.entityId);
        friendlyByteBuf.writeUtf(other.modelId);
        friendlyByteBuf.writeUtf(other.textureId);
        friendlyByteBuf.writeBoolean(other.disabled);
        S2CSyncPlayerStatePacket.encode(other.entityModelSync, friendlyByteBuf);
    }

    public static S2CSetModelAndTexturePacket decode(FriendlyByteBuf friendlyByteBuf) {
        return new S2CSetModelAndTexturePacket(friendlyByteBuf.readVarInt(), friendlyByteBuf.readUtf(), friendlyByteBuf.readUtf(), friendlyByteBuf.readBoolean(), S2CSyncPlayerStatePacket.decode(friendlyByteBuf));
    }

    public int entityId() {
        return entityId;
    }

    public String modelId() {
        return modelId;
    }

    public String textureId() {
        return textureId;
    }

    public boolean disabled() {
        return disabled;
    }

    public S2CSyncPlayerStatePacket entityModelSync() {
        return entityModelSync;
    }

    /** 客户端处理器委托，实现见 {@code client.net.ClientPayloadSinkImpl}。 */
    public static void handle(S2CSetModelAndTexturePacket other, PacketContext ctx) {
        ClientPayloadSink.Impl sink = ClientPayloadSink.get();
        if (sink != null) {
            sink.setModelAndTexture(other, ctx);
        }
    }
}