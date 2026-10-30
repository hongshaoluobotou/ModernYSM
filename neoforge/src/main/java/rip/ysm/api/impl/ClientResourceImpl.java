package rip.ysm.api.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * NeoForge 侧的客户端资源重载监听注册。
 *
 * <p>NeoForge 只在 {@code AddClientReloadListenersEvent}（mod 总线）内允许注册，
 * 而 common 侧（{@code RendererManager.register()}）在客户端入口点更早就会调用本类，
 * 故先入队，事件触发时统一注册。
 */
public final class ClientResourceImpl {

    private record Pending(Identifier id, ResourceManagerReloadListener listener) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();
    private static boolean hooked;

    private ClientResourceImpl() {
    }

    /** 由客户端入口点在 {@code @Mod} 构造器内调用（早于任何重载事件）。 */
    public static void init(IEventBus modBus) {
        if (hooked) {
            return;
        }
        hooked = true;
        modBus.addListener(AddClientReloadListenersEvent.class, event -> {
            for (Pending pending : PENDING) {
                event.addListener(pending.id(), pending.listener());
            }
            PENDING.clear();
        });
    }

    public static void registerReloadListener(Identifier id, ResourceManagerReloadListener listener) {
        PENDING.add(new Pending(id, listener));
    }
}
