package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.model.ServerModelUploadManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import rip.ysm.api.network.PacketContext;

public record C2SModelUploadChunkPacket(long uploadId, int offset, byte[] data) {

    public static void encode(C2SModelUploadChunkPacket message, FriendlyByteBuf buf) {
        buf.writeVarLong(message.uploadId);
        buf.writeVarInt(message.offset);
        buf.writeByteArray(message.data);
    }

    public static C2SModelUploadChunkPacket decode(FriendlyByteBuf buf) {
        return new C2SModelUploadChunkPacket(buf.readVarLong(), buf.readVarInt(), buf.readByteArray());
    }

    public static void handle(C2SModelUploadChunkPacket message, PacketContext ctx) {
        if (ctx.isServerSide()) {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                ServerModelUploadManager.onChunk(sender, message.uploadId(), message.offset(), message.data());
            }
        }
    }
}
