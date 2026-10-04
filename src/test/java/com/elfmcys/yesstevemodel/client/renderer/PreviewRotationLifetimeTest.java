package com.elfmcys.yesstevemodel.client.renderer;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import static org.junit.jupiter.api.Assertions.*;

class PreviewRotationLifetimeTest {
    @AfterEach
    void clearStates() {
        ModelPreviewRenderer.PREVIEW_YAW.clear();
        ModelPreviewRenderer.INVENTORY_PREVIEW_ROT.clear();
    }

    @Test
    void pendingStateKeepsBothRotationValuesAcrossGc() {
        EntityRenderState state = new EntityRenderState();
        ModelPreviewRenderer.PREVIEW_YAW.put(state, 213.5f);
        ModelPreviewRenderer.INVENTORY_PREVIEW_ROT.put(state, new float[]{180, 20, -10});
        System.gc();
        assertEquals(213.5f, ModelPreviewRenderer.PREVIEW_YAW.get(state).floatValue());
        assertArrayEquals(new float[]{180, 20, -10}, ModelPreviewRenderer.INVENTORY_PREVIEW_ROT.get(state));
        Reference.reachabilityFence(state);
    }

    @Test
    void fallbackOrCulledStateIsNotRetainedByEitherRotationMap() throws InterruptedException {
        WeakReference<EntityRenderState> ref = abandonedState();
        for (int i = 0; i < 100 && ref.get() != null; i++) {
            System.gc();
            Thread.sleep(10);
        }
        assertNull(ref.get(), "A state skipped by the custom renderer must still be collectible");
    }

    private static WeakReference<EntityRenderState> abandonedState() {
        EntityRenderState state = new EntityRenderState();
        ModelPreviewRenderer.PREVIEW_YAW.put(state, 213.5f);
        ModelPreviewRenderer.INVENTORY_PREVIEW_ROT.put(state, new float[]{180, 20, -10});
        return new WeakReference<>(state);
    }

    @Test
    void twoViewportsOfOneEntityKeepIndependentRotations() {
        EntityRenderState first = new EntityRenderState();
        EntityRenderState second = new EntityRenderState();
        ModelPreviewRenderer.PREVIEW_YAW.put(first, 180.0f);
        ModelPreviewRenderer.PREVIEW_YAW.put(second, 220.0f);
        assertEquals(180.0f, ModelPreviewRenderer.PREVIEW_YAW.remove(first).floatValue());
        assertEquals(220.0f, ModelPreviewRenderer.PREVIEW_YAW.remove(second).floatValue());
    }
}
