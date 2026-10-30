package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.network.ClientPayloadSink;
import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

public record S2CModelUploadResultPacket(long uploadId, byte status, String modelId, long h1, long h2, String message) {
    public static void encode(S2CModelUploadResultPacket packet, FriendlyByteBuf buf) {
        buf.writeVarLong(packet.uploadId);
        buf.writeByte(packet.status);
        buf.writeUtf(packet.modelId);
        buf.writeVarLong(packet.h1);
        buf.writeVarLong(packet.h2);
        buf.writeUtf(packet.message);
    }

    public static S2CModelUploadResultPacket decode(FriendlyByteBuf buf) {
        return new S2CModelUploadResultPacket(buf.readVarLong(), buf.readByte(), buf.readUtf(), buf.readVarLong(), buf.readVarLong(), buf.readUtf());
    }

    /** 客户端处理器委托，实现见 {@code client.net.ClientPayloadSinkImpl}。 */
    public static void handle(S2CModelUploadResultPacket packet, PacketContext ctx) {
        ClientPayloadSink.Impl sink = ClientPayloadSink.get();
        if (sink != null) {
            sink.modelUploadResult(packet, ctx);
        }
    }
}
