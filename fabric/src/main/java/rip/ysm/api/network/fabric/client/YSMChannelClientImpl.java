package rip.ysm.api.network.fabric.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.Identifier;
import rip.ysm.api.network.fabric.YSMChannelImpl;
import rip.ysm.api.network.fabric.YsmRawPayload;

public final class YSMChannelClientImpl {

    private YSMChannelClientImpl() {
    }

    public static void init(Identifier channelId) {
        // TODO port: 26.3 fabric-api payload 体系
        ClientPlayNetworking.registerGlobalReceiver(YsmRawPayload.TYPE, (payload, ctx) ->
                YSMChannelImpl.dispatch(new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(payload.data())),
                        new ClientPacketContext(ctx.client(), ctx.packetContext().orElseThrow(net.fabricmc.fabric.api.networking.v1.context.PacketContext.CONNECTION))));
    }

    public static void sendToServer(YsmRawPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    public static Packet<?> toServerboundPacket(YsmRawPayload payload) {
        return ClientPlayNetworking.createServerboundPacket(payload);
    }
}
