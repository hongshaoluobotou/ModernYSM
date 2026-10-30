package com.elfmcys.yesstevemodel.network;

import com.elfmcys.yesstevemodel.network.message.S2CExecuteMolangPacket;
import com.elfmcys.yesstevemodel.network.message.S2CModelSyncPayload;
import com.elfmcys.yesstevemodel.network.message.S2CModelUploadResultPacket;
import com.elfmcys.yesstevemodel.network.message.S2CModelUploadStartPacket;
import com.elfmcys.yesstevemodel.network.message.S2CSetModelAndTexturePacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncAnimationExpressionPacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncAuthModelsPacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncPlayerStatePacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncProjectileModelPacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncStarModelsPacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncVehicleModelPacket;
import com.elfmcys.yesstevemodel.network.message.S2CVersionCheckPacket;
import rip.ysm.api.network.PacketContext;

/**
 * 「客户端接收处理器」的中立分发点。
 *
 * <p><b>为什么需要这层抽象</b>：NeoForge/Forge 专用服务器的 classpath 里**没有** {@code net.minecraft.client.*}
 * （FML 的 DistCleaner 在类加载时直接抛 {@code NoClassDefFoundError}）。而网络层要在服务端注册
 * 双向 payload，注册时的 {@code S2CXxx::handle} 方法引用会让 JVM 链接 payload 类并校验其方法体——
 * 只要方法体里出现客户端类（{@code Minecraft}/ClientModelManager/…），服务端启动即崩。
 *
 * <p>所以所有 {@code S2C*} payload 类只保留 {@code encode/decode} 与一行委托，
 * 真正的处理逻辑在 {@code client.net.ClientPayloadSinkImpl}（只会在物理客户端加载并安装）。
 * Fabric 侧类路径同时包含客户端类，本层对它没有行为影响（同一实现）。
 */
public final class ClientPayloadSink {

    /** 客户端处理器契约（参数类型均为 common 里的 payload 类，不含任何客户端类型）。 */
    public interface Impl {
        void executeMolang(S2CExecuteMolangPacket message, PacketContext ctx);

        void modelSync(S2CModelSyncPayload message, PacketContext ctx);

        void modelUploadStart(S2CModelUploadStartPacket packet, PacketContext ctx);

        void modelUploadResult(S2CModelUploadResultPacket packet, PacketContext ctx);

        void setModelAndTexture(S2CSetModelAndTexturePacket message, PacketContext ctx);

        void animationExpression(S2CSyncAnimationExpressionPacket message, PacketContext ctx);

        void authModels(S2CSyncAuthModelsPacket message, PacketContext ctx);

        void playerState(S2CSyncPlayerStatePacket message, PacketContext ctx);

        void projectileModel(S2CSyncProjectileModelPacket message, PacketContext ctx);

        void starModels(S2CSyncStarModelsPacket message, PacketContext ctx);

        void vehicleModel(S2CSyncVehicleModelPacket message, PacketContext ctx);

        void versionCheck(S2CVersionCheckPacket message, PacketContext ctx);

        /** 版本检查包里 {@code open_ysm:v1} 品牌段的副作用（decode 期调用）。 */
        void oysmBrand(boolean allowUpload);
    }

    private static volatile Impl impl;

    private ClientPayloadSink() {
    }

    /** 由客户端入口点安装（专用服务器上保持为 null，S2C 处理器全部空转）。 */
    public static void install(Impl handler) {
        impl = handler;
    }

    public static Impl get() {
        return impl;
    }
}
