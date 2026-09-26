package com.elfmcys.yesstevemodel.mixin.client;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 阶段②：暴露 vanilla 私有 {@link RenderPipelines#register}，让 YsmRenderPipelines 的自定义管线
 * 进入启动期 shader 编译/预热清单（requiredPipelines）。wrap 点性质：仅反射入口，无注入逻辑。
 */
@Mixin(RenderPipelines.class)
public interface RenderPipelinesInvoker {

    @Invoker("register")
    static RenderPipeline register(RenderPipeline pipeline) {
        throw new AssertionError();
    }
}
