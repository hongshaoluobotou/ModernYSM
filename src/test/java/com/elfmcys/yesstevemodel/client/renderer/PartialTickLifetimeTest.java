package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.mixin.client.EntityRenderDispatcherMixin;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PartialTickLifetimeTest {
    @SuppressWarnings("unchecked")
    private static Map<EntityRenderState, Float> partialTicks() throws Exception {
        Field field = EntityRenderDispatcherMixin.class.getDeclaredField("ysm$stateToPartialTick");
        field.setAccessible(true);
        return (Map<EntityRenderState, Float>) field.get(null);
    }

    @AfterEach
    void clearStates() throws Exception {
        partialTicks().clear();
    }

    @Test
    void pendingStateKeepsItsCapturedPartialTickAcrossGc() throws Exception {
        EntityRenderState state = new EntityRenderState();
        partialTicks().put(state, 0.375f);
        for (int i = 0; i < 10; i++) {
            System.gc();
            Thread.sleep(10);
        }
        assertEquals(0.375f, partialTicks().getOrDefault(state, 0.0f).floatValue());
        Reference.reachabilityFence(state);
    }

    @Test
    void partialTickMapDoesNotKeepItsStateAlive() throws Exception {
        WeakReference<EntityRenderState> ref = abandonedState();
        for (int i = 0; i < 100 && ref.get() != null; i++) {
            System.gc();
            Thread.sleep(10);
        }
        assertNull(ref.get());
    }

    private static WeakReference<EntityRenderState> abandonedState() throws Exception {
        EntityRenderState state = new EntityRenderState();
        partialTicks().put(state, 0.375f);
        return new WeakReference<>(state);
    }
}
