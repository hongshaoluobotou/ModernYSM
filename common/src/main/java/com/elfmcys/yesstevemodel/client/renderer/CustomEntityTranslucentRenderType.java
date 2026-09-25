package com.elfmcys.yesstevemodel.client.renderer;

import net.minecraft.util.Util;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

/**
 * 26.3 移植说明：{@code RenderType} 不可再子类化（构造私有化，工厂统一在 {@code RenderTypes}），
 * 原先继承 {@code RenderType} 携带 useBlend/outline 状态的做法已不可行。
 * 现在仅保留按纹理缓存的 entityTranslucent RenderType；
 * 轮廓（outline）渲染由调用方通过 {@code RenderTypes.outline(...)} 单独提交。
 */
public final class CustomEntityTranslucentRenderType {

    private static final Function<Identifier, RenderType> CACHE = Util.memoize(
            resourceLocation -> RenderTypes.entityTranslucent(resourceLocation, false));

    private CustomEntityTranslucentRenderType() {
    }

    public static RenderType get(Identifier resourceLocation) {
        return CACHE.apply(resourceLocation);
    }
}
