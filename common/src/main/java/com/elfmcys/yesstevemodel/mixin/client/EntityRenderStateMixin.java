package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.client.renderer.PreviewRenderStateAccess;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** 由渲染状态持有等待提交的 GUI 预览，避免静态注册表延长其生命周期。 */
@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements PreviewRenderStateAccess {
    @Unique
    private Object ysm$previewOwner;

    @Override
    public void ysm$retainPreviewOwner(Object previewOwner) {
        this.ysm$previewOwner = previewOwner;
    }
}
