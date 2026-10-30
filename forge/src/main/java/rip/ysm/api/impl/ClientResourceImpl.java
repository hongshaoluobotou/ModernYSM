package rip.ysm.api.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge 侧的客户端资源重载监听注册（{@code RegisterClientReloadListenersEvent}）。
 *
 * <p>该事件由 Forge 在资源管理器创建时 {@code BUS.post(...)}，而 common 侧在 mod 构造期就会调用本类，
 * 故先入队，事件触发时统一注册（与 NeoForge/fabric 版语义一致）。
 */
public final class ClientResourceImpl {

    private record Pending(Identifier id, ResourceManagerReloadListener listener) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();
    private static boolean hooked;

    private ClientResourceImpl() {
    }

    public static void init() {
        if (hooked) {
            return;
        }
        hooked = true;
        RegisterClientReloadListenersEvent.BUS.addListener(event -> {
            for (Pending pending : PENDING) {
                // Forge 的事件只有 registerReloadListener(PreparableReloadListener)，没有 id 参数
                event.registerReloadListener(pending.listener());
            }
            PENDING.clear();
        });
    }

    public static void registerReloadListener(Identifier id, ResourceManagerReloadListener listener) {
        PENDING.add(new Pending(id, listener));
    }
}
