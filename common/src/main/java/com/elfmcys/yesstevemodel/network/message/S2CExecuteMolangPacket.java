package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.network.ClientPayloadSink;
import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

public class S2CExecuteMolangPacket {

    private final int[] entityIds;

    private final String expression;

    public S2CExecuteMolangPacket(int entityIds, String expression) {
        this.entityIds = new int[]{entityIds};
        this.expression = expression;
    }

    public S2CExecuteMolangPacket(int[] entityIds, String expression) {
        this.entityIds = entityIds;
        this.expression = expression;
    }

    public int[] entityIds() {
        return entityIds;
    }

    public String expression() {
        return expression;
    }

    public static void encode(S2CExecuteMolangPacket message, FriendlyByteBuf buf) {
        buf.writeVarIntArray(message.entityIds);
        buf.writeUtf(message.expression);
    }

    public static S2CExecuteMolangPacket decode(FriendlyByteBuf buf) {
        return new S2CExecuteMolangPacket(buf.readVarIntArray(), buf.readUtf());
    }

    /**
     * 客户端处理器委托：真正的实现（Minecraft / PlayerCapability 等客户端类）在
     * {@code client.net.ClientPayloadSinkImpl}。这样 payload 类在专用服务器上可安全加载。
     */
    public static void handle(S2CExecuteMolangPacket message, PacketContext ctx) {
        ClientPayloadSink.Impl sink = ClientPayloadSink.get();
        if (sink != null) {
            sink.executeMolang(message, ctx);
        }
    }
}
