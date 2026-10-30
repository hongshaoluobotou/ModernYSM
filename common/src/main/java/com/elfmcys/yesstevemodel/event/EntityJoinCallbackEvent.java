package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import rip.ysm.api.PlatformAPI;
import rip.ysm.api.event.ClientEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class EntityJoinCallbackEvent {

    private static final Cache<Integer, List<Consumer<Entity>>> callbackCache = CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.SECONDS).build();

    private EntityJoinCallbackEvent() {
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        // 原 Architectury EntityEvent.ADD（客户端侧）
        ClientEvents.onEntityLoad((entity, level) -> {
            if (!YesSteveModel.isAvailable() || !level.isClientSide()) {
                return;
            }
            List<Consumer<Entity>> list = callbackCache.getIfPresent(entity.getId());
            if (list != null) {
                for (Consumer<Entity> entityConsumer : list) {
                    entityConsumer.accept(entity);
                }
            }
            callbackCache.invalidate(entity.getId());
        });
    }

    public static void addCallback(int i, Consumer<Entity> consumer) {
        Minecraft.getInstance().execute(() -> {
            ClientLevel clientLevel = Minecraft.getInstance().level;
            if (clientLevel != null) {
                Entity entity = clientLevel.getEntity(i);
                if (entity != null) {
                    consumer.accept(entity);
                } else {
                    addToCallbackList(i, consumer);
                }
            }
        });
    }

    private static void addToCallbackList(int i, Consumer<Entity> consumer) {
        List<Consumer<Entity>> arrayList = callbackCache.getIfPresent(i);
        if (arrayList == null) {
            arrayList = new ArrayList<>(3);
            callbackCache.put(i, arrayList);
        }
        arrayList.add(consumer);
    }
}