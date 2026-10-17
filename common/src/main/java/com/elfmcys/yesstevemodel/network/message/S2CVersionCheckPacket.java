package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.ClientOnlyMode;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

public class S2CVersionCheckPacket {

    private static final String OPEN_YSM_BRAND = "open_ysm:v1";
    private static final String MODEL_SYNC_FRAGMENT_BRAND = "open_ysm:model_sync_fragments_v1";

    private final String version;
    private final boolean supportsModelSyncFragments;
    // 26.3 port：新增 open_ysm:v1 品牌发射（原实现 decode 能消费但 encode 从未写出，导致
    // 本模组自建服务端上的 isOysmServer/allowUpload 永远为 false、上传按钮永久禁用）。
    private final boolean oysmBrand;
    private final boolean allowUpload;

    public S2CVersionCheckPacket() {
        this(NetworkHandler.VERSION, true, true, allowUploadConfig());
    }

    private S2CVersionCheckPacket(String version, boolean supportsModelSyncFragments) {
        this(version, supportsModelSyncFragments, false, false);
    }

    private S2CVersionCheckPacket(String version, boolean supportsModelSyncFragments, boolean oysmBrand, boolean allowUpload) {
        this.version = version;
        this.supportsModelSyncFragments = supportsModelSyncFragments;
        this.oysmBrand = oysmBrand;
        this.allowUpload = allowUpload;
    }

    // 本模组服务端即 OpenYSM 协议服务端；配置未初始化时按默认（允许上传）处理。
    private static boolean allowUploadConfig() {
        return ServerConfig.ALLOW_UPLOAD == null || ServerConfig.ALLOW_UPLOAD.get();
    }

    public static S2CVersionCheckPacket decode(FriendlyByteBuf buf) {
        String version = buf.readUtf();
        boolean supportsModelSyncFragments = false;
        // 循环解析可选品牌段（本模组服务端会同时携带 fragments + open_ysm 两个品牌，先后顺序与
        // encode 一致；官方 OpenYSM 服务器可能只发其中一个，同样兼容）。未知品牌立即停止，
        // 避免把后续未知字节误读成 boolean。
        while (buf.readableBytes() > 0) {
            String brand = buf.readUtf();
            if (brand.equals(OPEN_YSM_BRAND)) {
                ClientModelManager.setOysmServer(true);
                if (buf.readableBytes() > 0) {
                    ClientModelManager.setAllowUpload(buf.readBoolean());
                } else {
                    ClientModelManager.setAllowUpload(true);
                }
            } else if (brand.equals(MODEL_SYNC_FRAGMENT_BRAND)) {
                supportsModelSyncFragments = true;
            } else {
                break;
            }
        }
        return new S2CVersionCheckPacket(version, supportsModelSyncFragments);
    }

    public static void encode(S2CVersionCheckPacket message, FriendlyByteBuf buf) {
        buf.writeUtf(message.version);
        if (message.supportsModelSyncFragments) {
            buf.writeUtf(MODEL_SYNC_FRAGMENT_BRAND);
        }
        if (message.oysmBrand) {
            buf.writeUtf(OPEN_YSM_BRAND);
            buf.writeBoolean(message.allowUpload);
        }
    }

    public static void handle(S2CVersionCheckPacket message, PacketContext ctx) {
        NetworkHandler.setServerSupportsModelSyncFragments(message.supportsModelSyncFragments);
        if (NetworkHandler.setChannelVersion(ctx.getConnection(), message.version)) {
            ctx.enqueueWork(() -> {
                ClientOnlyMode.leaveStandalone();
                ClientModelManager.onSyncConnected();
            });
        }
        if (NetworkHandler.VERSION.equals(message.version)) {
            NetworkHandler.markClientHandshakeComplete();
        }
        NetworkHandler.sendVersionCheck(ctx.getConnection());
    }
}
