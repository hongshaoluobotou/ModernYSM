package rip.ysm.api.network.fabric;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// TODO port: 26.3 fabric-api 改为 CustomPacketPayload 体系，用此包装 payload 承载原 discriminator+bytes 通道
public record YsmRawPayload(byte[] data) implements CustomPacketPayload {

    public static Type<YsmRawPayload> TYPE;

    public static final StreamCodec<FriendlyByteBuf, YsmRawPayload> CODEC = StreamCodec.ofMember(
            (payload, buf) -> buf.writeByteArray(payload.data),
            buf -> new YsmRawPayload(buf.readByteArray()));

    public static void init(Identifier channelId) {
        TYPE = new Type<>(channelId);
    }

    public static YsmRawPayload of(FriendlyByteBuf buf) {
        // 复制剩余字节并释放（调用方 buf 可能是 DirectBuf）
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), bytes);
        return new YsmRawPayload(bytes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
