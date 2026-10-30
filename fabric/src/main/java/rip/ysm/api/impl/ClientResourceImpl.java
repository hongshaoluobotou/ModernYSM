package rip.ysm.api.impl;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Fabric 侧的客户端资源重载监听注册（{@code ResourceLoader} + CLIENT_RESOURCES）。
 */
public final class ClientResourceImpl {

    private ClientResourceImpl() {
    }

    public static void registerReloadListener(Identifier id, ResourceManagerReloadListener listener) {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(id, listener);
    }
}
