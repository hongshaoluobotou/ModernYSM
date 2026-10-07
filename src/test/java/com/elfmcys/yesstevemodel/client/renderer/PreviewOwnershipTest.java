package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.client.entity.PlayerPreviewEntity;
import com.elfmcys.yesstevemodel.client.gui.PlayerModelScreen;
import com.elfmcys.yesstevemodel.client.gui.PlayerTextureScreen;
import com.elfmcys.yesstevemodel.mixin.client.EntityRenderStateMixin;
import org.junit.jupiter.api.Test;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class PreviewOwnershipTest {
    @Test
    void pendingStateKeepsItsPreviewAliveAfterTheScreenReleasesIt() {
        EntityRenderStateMixin state = new EntityRenderStateMixin();
        WeakReference<Object> owner = attachPreview(state);
        System.gc();
        assertNotNull(owner.get(), "The pending PiP state must retain its preview");
        Reference.reachabilityFence(state);
    }

    @Test
    void abandonedStateReleasesItsPreviewWithoutAnotherRenderOrMapCleanup() throws InterruptedException {
        WeakReference<?>[] refs = abandonedState();
        awaitCollected(refs);
    }

    @Test
    void stateReuseReleasesItsPreviousPreview() throws InterruptedException {
        EntityRenderStateMixin state = new EntityRenderStateMixin();
        WeakReference<Object> owner = attachPreview(state);
        state.ysm$retainPreviewOwner(null);
        awaitCollected(owner);
        Reference.reachabilityFence(state);
    }

    @Test
    void previewsAreOwnedByTheirScreensAndDummyPlayer() throws Exception {
        assertFalse(Modifier.isStatic(PlayerModelScreen.class.getDeclaredField("previewHolders").getModifiers()));
        assertFalse(Modifier.isStatic(PlayerTextureScreen.class.getDeclaredField("texturePreviewHolders").getModifiers()));
        assertThrows(NoSuchFieldException.class, () -> PlayerPreviewEntity.class.getDeclaredField("PREVIEW_WRAPPERS"));
        Class<?> dummy = Class.forName(PlayerPreviewEntity.class.getName() + "$DummyPlayer", false,
                PlayerPreviewEntity.class.getClassLoader());
        assertEquals(PlayerPreviewEntity.class, dummy.getDeclaredField("previewWrapper").getType());
        assertFalse(Modifier.isStatic(dummy.getDeclaredField("previewWrapper").getModifiers()));
    }

    private static WeakReference<Object> attachPreview(PreviewRenderStateAccess state) {
        Object owner = new Object();
        state.ysm$retainPreviewOwner(owner);
        return new WeakReference<>(owner);
    }

    private static WeakReference<?>[] abandonedState() {
        EntityRenderStateMixin state = new EntityRenderStateMixin();
        return new WeakReference<?>[]{new WeakReference<>(state), attachPreview(state)};
    }

    private static void awaitCollected(WeakReference<?>... refs) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            System.gc();
            if (java.util.Arrays.stream(refs).allMatch(ref -> ref.get() == null)) return;
            Thread.sleep(10);
        }
        for (WeakReference<?> ref : refs) assertNull(ref.get(), "Preview ownership must not escape its state");
    }
}
