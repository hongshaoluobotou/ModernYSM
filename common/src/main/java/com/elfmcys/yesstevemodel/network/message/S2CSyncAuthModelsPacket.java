package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.network.ClientPayloadSink;
import com.google.common.collect.Sets;
import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

import java.util.HashSet;
import java.util.Set;

public class S2CSyncAuthModelsPacket {

    private final Set<String> authModels;

    public S2CSyncAuthModelsPacket(Set<String> authModels) {
        this.authModels = authModels;
    }

    public static void encode(S2CSyncAuthModelsPacket message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.authModels.size());
        for (String modelId : message.authModels) {
            buf.writeUtf(modelId);
        }
    }

    public static S2CSyncAuthModelsPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        HashSet<String> tmp = Sets.newHashSet();
        for (int i = 0; i < size; i++) {
            tmp.add(buf.readUtf());
        }
        return new S2CSyncAuthModelsPacket(tmp);
    }

    public Set<String> authModels() {
        return authModels;
    }

    /** 客户端处理器委托，实现见 {@code client.net.ClientPayloadSinkImpl}。 */
    public static void handle(S2CSyncAuthModelsPacket message, PacketContext ctx) {
        ClientPayloadSink.Impl sink = ClientPayloadSink.get();
        if (sink != null) {
            sink.authModels(message, ctx);
        }
    }
}