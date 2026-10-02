package com.elfmcys.yesstevemodel.client.renderer;

import com.google.common.collect.MapMaker;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 保存 extract 到 submit 之间的实体与插值时间。
 * 弱键不会延长 render state 生命周期；强值保证仍在使用的 state 不会因 GC 丢失数据。
 * 特别是 PiP 提取后即使界面已关闭，state 仍需持有 DummyPlayer 及其预览包装器。
 */
public final class EntityRenderStateBridge {
    private static final Map<EntityRenderState, CapturedEntity> ENTITIES = new MapMaker().weakKeys().makeMap();

    private EntityRenderStateBridge() {
    }

    public static void capture(EntityRenderState state, Entity entity, float partialTick) {
        ENTITIES.put(state, new CapturedEntity(entity, partialTick));
    }

    @Nullable
    public static CapturedEntity get(EntityRenderState state) {
        return ENTITIES.get(state);
    }

    /** 离服/停止客户端时立即释放最后一帧，避免等待弱键表下一次访问才清理。 */
    public static void clear() {
        ENTITIES.clear();
    }

    public record CapturedEntity(Entity entity, float partialTick) {
    }
}
