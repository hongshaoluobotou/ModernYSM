package com.elfmcys.yesstevemodel.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.BindGroupLayouts;
import com.elfmcys.yesstevemodel.mixin.client.RenderPipelinesInvoker;
import net.minecraft.resources.Identifier;

/**
 * 阶段②（renderpearl 原生适配路线图，2026.09.27）：YSM 自定义 RenderPipeline 集。
 *
 * <p>全部只面向公开锚点 API（{@link RenderPipeline} / {@code RenderSetup}）编程，
 * 不写任何 OpenGL/Vulkan 挡位分支——后端差异由 Mojang renderpearl 抽象层承担。</p>
 *
 * <p>定义方式逐字节复刻 vanilla {@code RenderPipelines}（javap -c 反编译 26.3 确认）：
 * 私有 snippet 链不可复用，故按相同配方重建——
 * {@code ENTITY_SNIPPET = builder(GLOBALS).withBindGroupLayouts(PROJECTION, DYNAMIC_TRANSFORMS, FOG)
 * .withVertex/FragmentShader("core/entity").withBindGroupLayout(SAMPLER0)
 * .withVertexBinding(0, DefaultVertexFormat.ENTITY).withPrimitiveTopology(QUADS)
 * .withDepthStencilState(DepthStencilState.DEFAULT)}；
 * 具体管线在其上追加 SAMPLER1（overlay）、ALPHA_CUTOUT / PER_FACE_LIGHTING / EMISSIVE define、
 * cull 开关与混合。shader 一律复用 vanilla {@code core/entity}，不自己写 GLSL。</p>
 *
 * <p>管线必须经 vanilla 私有 {@code RenderPipelines.register} 注册（{@link RenderPipelinesInvoker}），
 * 才能进入 requiredPipelines 的启动期 shader 编译/预热。</p>
 *
 * <p><b>变体清单</b>（映射表见 {@link YsmRenderTypes}）：
 * <ul>
 *   <li>{@link #GEO_CUTOUT_CULL} — 不透明单面（cull back，GPU 栅格化剔除，替代 TODO 9 已删的 CPU 投影行列式剔除）</li>
 *   <li>{@link #GEO_CUTOUT_NO_CULL} — 不透明双面（过渡期：ysmGlow 零厚度面片等不可判向几何仍需双面）</li>
 *   <li>{@link #GEO_TRANSLUCENT} — 半透明（BlendFunction.TRANSLUCENT + 双面，RenderSetup 层 sortOnUpload 对齐 1.20.1 entityTranslucent）</li>
 *   <li>{@link #GEO_GLOW} — 全亮变体（EMISSIVE define）。<b>暂未接线</b>：ysmGlow 是"骨骼级"粒度
 *       （同一 mesh 内 glow/非 glow 骨骼交错），现仍以 FULLBRIGHT 光值 0xF000F0 逐顶点模拟（NativeModelRenderer）；
 *       等阶段③ flush 收敛时按 glow 骨骼分批后再切本管线。</li>
 * </ul></p>
 */
public final class YsmRenderPipelines {

    private static final Identifier ENTITY_SHADER = Identifier.withDefaultNamespace("core/entity");

    /** 复刻 vanilla 私有 ENTITY_SNIPPET：实体坐标系 + 纹理 + 光照/雾矩阵锚点。 */
    private static final RenderPipeline.Snippet GEO_BASE = RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(BindGroupLayouts.FOG)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            // vanilla ENTITY_SNIPPET 后半段（javap 实证）：LIGHTING（shader 的 Lighting uniform）+
            // SAMPLER2（光照贴图）——缺任何一个 shader 编译即报 "Unable to find shader defined uniform (Lighting)"。
            .withBindGroupLayout(BindGroupLayouts.LIGHTING)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
            .withVertexShader(ENTITY_SHADER)
            .withFragmentShader(ENTITY_SHADER)
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .buildSnippet();

    public static final RenderPipeline GEO_CUTOUT_CULL = RenderPipelinesInvoker.register(RenderPipeline.builder(GEO_BASE)
            .withLocation(Identifier.fromNamespaceAndPath("yes_steve_model", "pipeline/geo_cutout_cull"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .build());

    public static final RenderPipeline GEO_CUTOUT_NO_CULL = RenderPipelinesInvoker.register(RenderPipeline.builder(GEO_BASE)
            .withLocation(Identifier.fromNamespaceAndPath("yes_steve_model", "pipeline/geo_cutout_no_cull"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withShaderDefine("PER_FACE_LIGHTING")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .withCull(false)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .build());

    /** 对齐 vanilla {@code pipeline/entity_translucent}（半透明 + 双面 + PER_FACE_LIGHTING）。 */
    public static final RenderPipeline GEO_TRANSLUCENT = RenderPipelinesInvoker.register(RenderPipeline.builder(GEO_BASE)
            .withLocation(Identifier.fromNamespaceAndPath("yes_steve_model", "pipeline/geo_translucent"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withShaderDefine("PER_FACE_LIGHTING")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .withCull(false)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build());

    /** 全亮变体（vanilla EMISSIVE define 语义），暂未接线，见类注释。 */
    public static final RenderPipeline GEO_GLOW = RenderPipelinesInvoker.register(RenderPipeline.builder(GEO_BASE)
            .withLocation(Identifier.fromNamespaceAndPath("yes_steve_model", "pipeline/geo_glow"))
            .withShaderDefine("EMISSIVE")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .withCull(false)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .build());

    private YsmRenderPipelines() {
    }

    /**
     * 显式触发类初始化（管线常量注册）。从 client entrypoint 早期调用，
     * 保证自定义管线在 vanilla requiredPipelines 消费（资源加载/shader 预热）之前就位。
     */
    public static void init() {
    }
}
