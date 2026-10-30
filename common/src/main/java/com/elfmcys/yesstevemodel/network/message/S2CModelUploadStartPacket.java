package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.network.ClientPayloadSink;
import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

public record S2CModelUploadStartPacket(long uploadId, byte status, int chunkSize, int maxTotalBytes, int chunksPerTick, String message) {
    public static void encode(S2CModelUploadStartPacket packet, FriendlyByteBuf buf) {
        buf.writeVarLong(packet.uploadId);
        buf.writeByte(packet.status);
        buf.writeVarInt(packet.chunkSize);
        buf.writeVarInt(packet.maxTotalBytes);
        buf.writeVarInt(packet.chunksPerTick);
        buf.writeUtf(packet.message);
    }

    public static S2CModelUploadStartPacket decode(FriendlyByteBuf buf) {
        long uploadId = buf.readVarLong();
        byte status = buf.readByte();
        int chunkSize = buf.readVarInt();
        int maxTotalBytes = buf.readVarInt();
        int chunksPerTick = buf.readVarInt();
        String message = buf.readUtf();
        return new S2CModelUploadStartPacket(uploadId, status, chunkSize, maxTotalBytes, chunksPerTick, message);
    }

    /** 客户端处理器委托，实现见 {@code client.net.ClientPayloadSinkImpl}。 */
    public static void handle(S2CModelUploadStartPacket packet, PacketContext ctx) {
        ClientPayloadSink.Impl sink = ClientPayloadSink.get();
        if (sink != null) {
            sink.modelUploadStart(packet, ctx);
        }
    }
}
