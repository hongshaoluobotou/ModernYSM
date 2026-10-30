package rip.ysm.api.network.impl.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import rip.ysm.api.network.impl.PayloadPacketContext;
import rip.ysm.api.network.impl.YSMChannelImpl;
import rip.ysm.api.network.impl.YsmRawPayload;

/**
 * NeoForge 侧的客户端通道实现。
 *
 * <p>与 fabric 版的差异：NeoForge 的 payload 收发统一走 {@code PacketDistributor} /
 * {@code ServerboundCustomPayloadPacket}，payload 类型本身在双向注册时就已经带上了客户端处理器
 * （见 {@link YSMChannelImpl#init}），因此这里的 {@code init} 只负责把客户端处理器接进通道。
 *
 * <p>本类是客户端专属代码，只会在物理客户端加载。
 */
public final class YSMChannelClientImpl {

    private YSMChannelClientImpl() {
    }

    public static void init(Identifier channelId) {
        YSMChannelImpl.setClientHandler((payload, context) -> YSMChannelImpl.dispatch(
                new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(payload.data())),
                new PayloadPacketContext(context, true)));
    }

    public static void sendToServer(YsmRawPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    public static Packet<?> toServerboundPacket(YsmRawPayload payload) {
        return new ServerboundCustomPayloadPacket(payload);
    }
}
