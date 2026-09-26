package com.elfmcys.yesstevemodel.mixin.client;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 阶段②：RenderType 构造在 26.3 被收拢为包私有 {@code create(String, RenderSetup)} + 私有构造，
 * vanilla 外无法建自定义 RenderType；此处暴露 create 供 YsmRenderTypes 构建 YSM 管线 RenderType，
 * 并暴露 state 供 flush 侧把 vanilla 实体 RenderType 映射为 YSM 等价类型。wrap 点性质：仅反射入口。
 */
@Mixin(RenderType.class)
public interface RenderTypeAccessor {

    @Invoker("create")
    static RenderType ysm$create(String name, RenderSetup setup) {
        throw new AssertionError();
    }

    @Accessor("state")
    RenderSetup ysm$getSetup();
}
