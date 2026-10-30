package rip.ysm.api.client;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * 客户端资源重载监听的加载器无关入口。
 *
 * <p>Fabric 对应 {@code ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(id, listener)}；
 * NeoForge/Forge 对应 {@code AddClientReloadListenersEvent}。
 */
public final class ClientResourceEvents {

    private ClientResourceEvents() {
    }

    /** 注册客户端资源重载监听器；同一 id 重复注册会抛异常（与 fabric-api 一致）。 */
    public static void registerReloadListener(Identifier id, ResourceManagerReloadListener listener) {
        rip.ysm.api.impl.ClientResourceImpl.registerReloadListener(id, listener);
    }
}
