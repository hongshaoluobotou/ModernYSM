package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.model.ServerModelUploadManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import rip.ysm.api.network.PacketContext;

public record C2SModelUploadFinishPacket(long uploadId) {

    public static void encode(C2SModelUploadFinishPacket message, FriendlyByteBuf buf) {
        buf.writeVarLong(message.uploadId);
    }

    public static C2SModelUploadFinishPacket decode(FriendlyByteBuf buf) {
        return new C2SModelUploadFinishPacket(buf.readVarLong());
    }

    public static void handle(C2SModelUploadFinishPacket message, PacketContext ctx) {
        if (ctx.isServerSide()) {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                ServerModelUploadManager.onFinish(sender, message.uploadId());
            }
        }
    }
}
