package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.network.ClientPayloadSink;
import com.google.common.collect.Sets;
import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

import java.util.HashSet;
import java.util.Set;

public class S2CSyncStarModelsPacket {

    private final Set<String> starModels;

    public S2CSyncStarModelsPacket(Set<String> starModels) {
        this.starModels = starModels;
    }

    public static void encode(S2CSyncStarModelsPacket message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.starModels.size());
        for (String starModel : message.starModels) {
            buf.writeUtf(starModel);
        }
    }

    public static S2CSyncStarModelsPacket decode(FriendlyByteBuf buf) {
        int varInt = buf.readVarInt();
        HashSet<String> tmp = Sets.newHashSet();
        for (int i = 0; i < varInt; i++) {
            tmp.add(buf.readUtf());
        }
        return new S2CSyncStarModelsPacket(tmp);
    }

    public Set<String> starModels() {
        return starModels;
    }

    /** 客户端处理器委托，实现见 {@code client.net.ClientPayloadSinkImpl}。 */
    public static void handle(S2CSyncStarModelsPacket message, PacketContext ctx) {
        ClientPayloadSink.Impl sink = ClientPayloadSink.get();
        if (sink != null) {
            sink.starModels(message, ctx);
        }
    }
}