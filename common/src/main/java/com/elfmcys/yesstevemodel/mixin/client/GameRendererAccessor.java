package com.elfmcys.yesstevemodel.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// 26.3：投影矩阵存在 GameRenderer 私有的 levelProjectionMatrixBuffer 中（旧 RenderSystem.getProjectionMatrix()
// 已删除）。背面剔除（NativeModelRenderer）需要真实投影矩阵恢复 1.20.1 的 det 判定语义。
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

    @Accessor("levelProjectionMatrixBuffer")
    ProjectionMatrixBuffer ysm$getLevelProjectionMatrixBuffer();
}
