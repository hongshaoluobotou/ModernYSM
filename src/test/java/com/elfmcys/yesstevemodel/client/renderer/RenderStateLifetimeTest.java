package com.elfmcys.yesstevemodel.client.renderer;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import static org.junit.jupiter.api.Assertions.*;

class RenderStateLifetimeTest {
    @AfterEach
    void clearStates() {
        EntityRenderStateBridge.clear();
        ModelPreviewRenderer.clearRenderStates();
    }

    @Test
    void activeStateKeepsCapturedTimeAndPreviewRotationAcrossGc() {
        EntityRenderState state = new EntityRenderState();
        // 这里只测试 extract/submit 桥接的所有权，不需要创建实体或启动 Minecraft。
        EntityRenderStateBridge.capture(state, null, 0.375f);
        ModelPreviewRenderer.PREVIEW_YAW.put(state, 213.5f);
        ModelPreviewRenderer.INVENTORY_PREVIEW_ROT.put(state, new float[]{180, 20, -10});

        System.gc();

        assertNotNull(EntityRenderStateBridge.get(state));
        assertEquals(0.375f, EntityRenderStateBridge.get(state).partialTick());
        assertEquals(213.5f, ModelPreviewRenderer.PREVIEW_YAW.get(state).floatValue());
        assertArrayEquals(new float[]{180, 20, -10}, ModelPreviewRenderer.INVENTORY_PREVIEW_ROT.get(state));
        Reference.reachabilityFence(state);
    }

    @Test
    void abandonedPreviewStateAndItsCapturedContextCanBeCollected() throws InterruptedException {
        WeakReference<?>[] references = captureAbandonedState();
        for (int i = 0; i < 100 && (references[0].get() != null || references[1].get() != null); i++) {
            System.gc();
            Thread.sleep(10);
            // Guava 按 segment 清理弱键；模拟后续帧的新 state，遍历不同 hash segment。
            EntityRenderState nextFrame = new EntityRenderState();
            EntityRenderStateBridge.capture(nextFrame, null, 0.5f);
            ModelPreviewRenderer.PREVIEW_YAW.put(nextFrame, 90.0f);
            ModelPreviewRenderer.INVENTORY_PREVIEW_ROT.put(nextFrame, new float[3]);
        }
        assertNull(references[0].get(), "未接管/被裁掉的预览不能被静态旋转表保留");
        assertNull(references[1].get(), "state 回收后必须释放它所拥有的提取上下文");
    }

    private static WeakReference<?>[] captureAbandonedState() {
        EntityRenderState state = new EntityRenderState();
        EntityRenderStateBridge.capture(state, null, 0.375f);
        ModelPreviewRenderer.PREVIEW_YAW.put(state, 213.5f);
        ModelPreviewRenderer.INVENTORY_PREVIEW_ROT.put(state, new float[]{180, 20, -10});
        return new WeakReference<?>[]{new WeakReference<>(state), new WeakReference<>(EntityRenderStateBridge.get(state))};
    }

    @Test
    void fallbackSubmissionAndDisconnectReleasePendingRotation() {
        EntityRenderState state = new EntityRenderState();
        ModelPreviewRenderer.PREVIEW_YAW.put(state, 213.5f);
        ModelPreviewRenderer.INVENTORY_PREVIEW_ROT.put(state, new float[]{180, 20, -10});

        ModelPreviewRenderer.releaseRenderState(state);

        assertFalse(ModelPreviewRenderer.PREVIEW_YAW.containsKey(state));
        assertFalse(ModelPreviewRenderer.INVENTORY_PREVIEW_ROT.containsKey(state));

        EntityRenderStateBridge.capture(state, null, 0.375f);
        ModelPreviewRenderer.PREVIEW_YAW.put(state, 90.0f);
        ModelPreviewRenderer.INVENTORY_PREVIEW_ROT.put(state, new float[3]);
        EntityRenderStateBridge.clear();
        ModelPreviewRenderer.clearRenderStates();
        assertNull(EntityRenderStateBridge.get(state));
        assertTrue(ModelPreviewRenderer.PREVIEW_YAW.isEmpty());
        assertTrue(ModelPreviewRenderer.INVENTORY_PREVIEW_ROT.isEmpty());
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
