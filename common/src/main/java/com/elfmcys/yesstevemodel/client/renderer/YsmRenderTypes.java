package com.elfmcys.yesstevemodel.client.renderer;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup.OutlineProperty;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

/**
 * 阶段②：YSM 自定义 RenderType 集（管线定义见 {@link YsmRenderPipelines}）。
 *
 * <p>26.3 的 {@code RenderType.create} 已包私有化（见 {@code mixin/client.RenderTypeAccessor}），
 * 本类用 vanilla 相同的 {@code RenderSetup.builder(pipeline)} 配方构建 YSM RenderType，
 * setup 参数逐字节对照 vanilla {@code RenderTypes} 的对应 lambda（javap -c 反编译确认）。</p>
 *
 * <p><b>RenderType → 管线映射表</b>（与 1.20.1 RenderType 分层语义对照）：
 * <ul>
 *   <li>{@code entityCutoutCull}（1.20.1 SOLID 相、GPU cull back）→ {@link #entityCutoutCull}（YsmRenderPipelines.GEO_CUTOUT_CULL，无混合 → solid 相）</li>
 *   <li>{@code armorCutoutNoCull}（1.20.1 SOLID 相、双面）→ {@link #entityCutoutNoCull} / {@link #armorCutoutNoCull}（GEO_CUTOUT_NO_CULL，无混合 → solid 相）</li>
 *   <li>{@code entityTranslucent}（1.20.1 TRANSLUCENT 相、按距离排序）→ {@link #entityTranslucent}（GEO_TRANSLUCENT + RenderSetup.sortOnUpload → 半透明相，排序语义与 1.20.1 对齐：
 *       混合 RenderType 进半透明阶段，sortOnUpload 提供逐 quad 距离排序；不透明类型无混合留 solid 相，与 1.20.1 分层一致）</li>
 * </ul></p>
 *
 * <p>不透明单面 cull 由管线 cull 状态在 GPU 栅格化阶段完成（TODO 9 的 CPU 投影行列式剔除已永久废弃）。</p>
 */
public final class YsmRenderTypes {

    /**
     * Iris（光影）兼容模式：Iris 按 vanilla RenderType 拦截/接管渲染，自定义 RenderPipeline
     * 不在其感知范围内（YSM 模型在光影下不渲染/异常）。检测到 Iris 时本类四个工厂全部回退
     * vanilla RenderType（阶段②之前的提交路径，Iris 已验证兼容），自定义管线不注册。
     */
    public static final boolean IRIS_LOADED = FabricLoader.getInstance().isModLoaded("iris");

    private static final Function<Identifier, RenderType> CUTOUT_CULL = Util.memoize(YsmRenderTypes::createCutoutCull);
    private static final Function<Identifier, RenderType> CUTOUT_NO_CULL = Util.memoize(YsmRenderTypes::createCutoutNoCull);
    private static final Function<Identifier, RenderType> TRANSLUCENT = Util.memoize(YsmRenderTypes::createTranslucent);
    private static final Function<Identifier, RenderType> ARMOR_CUTOUT_NO_CULL = Util.memoize(YsmRenderTypes::createArmorCutoutNoCull);

    private YsmRenderTypes() {
    }

    /** 不透明单面（cull back），替代 {@code RenderTypes.entityCutoutCull}。 */
    public static RenderType entityCutoutCull(Identifier texture) {
        if (IRIS_LOADED) {
            return RenderTypes.entityCutoutCull(texture);
        }
        return CUTOUT_CULL.apply(texture);
    }

    /** 不透明双面（过渡期），替代 {@code RenderTypes.entityCutout}。 */
    public static RenderType entityCutoutNoCull(Identifier texture) {
        if (IRIS_LOADED) {
            return RenderTypes.entityCutout(texture);
        }
        return CUTOUT_NO_CULL.apply(texture);
    }

    /** 半透明（混合 + sortOnUpload），替代 {@code CustomEntityTranslucentRenderType.get} / {@code RenderTypes.entityTranslucent}。 */
    public static RenderType entityTranslucent(Identifier texture) {
        if (IRIS_LOADED) {
            return RenderTypes.entityTranslucent(texture, false);
        }
        return TRANSLUCENT.apply(texture);
    }

    /** 双面盔甲/披风类，替代 {@code RenderTypes.armorCutoutNoCull}。 */
    public static RenderType armorCutoutNoCull(Identifier texture) {
        if (IRIS_LOADED) {
            return RenderTypes.armorCutoutNoCull(texture);
        }
        return ARMOR_CUTOUT_NO_CULL.apply(texture);
    }

    public static boolean isYsm(RenderType renderType) {
        return "yes_steve_model".equals(renderType.pipeline().getLocation().getNamespace());
    }

    // setup 配方逐项对照 vanilla RenderTypes.lambda$static$9（entity_cutout_cull）：
    // Sampler0 + useLightmap + useOverlay + affectsCrumbling + AFFECTS_OUTLINE。
    private static RenderType createCutoutCull(Identifier texture) {
        return create("geo_cutout_cull", YsmRenderPipelines.GEO_CUTOUT_CULL, texture, false, true);
    }

    // 对照 vanilla entity_cutout / armor_cutout_no_cull lambda（双面 + VIEW_OFFSET_Z_LAYERING）。
    private static RenderType createCutoutNoCull(Identifier texture) {
        RenderSetup setup = RenderSetup.builder(YsmRenderPipelines.GEO_CUTOUT_NO_CULL)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .useOverlay()
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .affectsCrumbling()
                .setOutline(OutlineProperty.AFFECTS_OUTLINE)
                .createRenderSetup();
        return createRenderType("geo_cutout_no_cull", setup);
    }

    private static RenderType createArmorCutoutNoCull(Identifier texture) {
        return create("geo_armor_cutout_no_cull", YsmRenderPipelines.GEO_CUTOUT_NO_CULL, texture, false, true);
    }

    // 对照 vanilla lambda$static$18（entity_translucent）：
    // Sampler0 + useLightmap + useOverlay + affectsCrumbling + sortOnUpload + outline NONE。
    // OIT（order-independent transparency）档位暂不挂（RenderPipelines.OIT_ENTITY 为 vanilla OitPipelineSet，
    // 其两套管线由 vanilla 私有 snippet 组成，无法为本 mod 管线配置；OIT 开启时回落普通混合，与 1.20.1 行为一致）。
    private static RenderType createTranslucent(Identifier texture) {
        return create("geo_translucent", YsmRenderPipelines.GEO_TRANSLUCENT, texture, true, false);
    }

    private static RenderType create(String name, com.mojang.renderpearl.api.pipeline.RenderPipeline pipeline,
                                     Identifier texture, boolean sortOnUpload, boolean affectsOutline) {
        RenderSetup.RenderSetupBuilder builder = RenderSetup.builder(pipeline)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .useOverlay()
                .affectsCrumbling();
        if (sortOnUpload) {
            builder.sortOnUpload();
        }
        builder.setOutline(affectsOutline ? OutlineProperty.AFFECTS_OUTLINE : OutlineProperty.NONE);
        return createRenderType(name, builder.createRenderSetup());
    }

    /** RenderTypeAccessor 是 mixin 接口（@Invoker 静态方法体由 mixin 填充）。 */
    private static RenderType createRenderType(String name, RenderSetup setup) {
        return com.elfmcys.yesstevemodel.mixin.client.RenderTypeAccessor.ysm$create(name, setup);
    }
}
