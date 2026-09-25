package com.elfmcys.yesstevemodel.mixin.client;

import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ProjectionMatrixBuffer.class)
public interface ProjectionMatrixBufferAccessor {

    @Accessor("lastUploadedProjection")
    Projection ysm$getLastUploadedProjection();
}
