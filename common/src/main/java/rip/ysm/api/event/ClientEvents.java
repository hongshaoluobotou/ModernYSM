package rip.ysm.api.event;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

import java.util.function.BiConsumer;

/**
 * 客户端生命周期事件的加载器无关入口（对应 Fabric 事件见各方法注释）。
 *
 * <p>注意：这些回调只在物理客户端注册；专用服务器上不会触发（与 fabric-api 行为一致）。
 */
public final class ClientEvents {

    private ClientEvents() {
    }

    /** 客户端 tick 结束（{@code ClientTickEvents.END_CLIENT_TICK}）。 */
    public static void onTick(Runnable callback) {
        rip.ysm.api.impl.EventBusImpl.onClientTick(callback);
    }

    /** 客户端 tick 开始（{@code ClientTickEvents.START_CLIENT_TICK}）。 */
    public static void onTickStart(Runnable callback) {
        rip.ysm.api.impl.EventBusImpl.onClientTickStart(callback);
    }

    /** 实体进入客户端世界（{@code ClientEntityEvents.ENTITY_LOAD}）。 */
    public static void onEntityLoad(BiConsumer<Entity, ClientLevel> callback) {
        rip.ysm.api.impl.EventBusImpl.onClientEntityLoad(callback);
    }

    /** 进入服务器/世界（{@code ClientPlayConnectionEvents.JOIN}）。 */
    public static void onPlayJoin(Runnable callback) {
        rip.ysm.api.impl.EventBusImpl.onClientPlayJoin(callback);
    }

    /** 断开服务器/世界（{@code ClientPlayConnectionEvents.DISCONNECT}）。 */
    public static void onPlayDisconnect(Runnable callback) {
        rip.ysm.api.impl.EventBusImpl.onClientPlayDisconnect(callback);
    }
}
