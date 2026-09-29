package com.elfmcys.yesstevemodel.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 26.3 移植：客户端渲染已改为 EntityRenderState + submit/GpuBuffer（renderpearl）体系，
 * {@code MultiBufferSource}/{@code BufferSource} 被移除，无法再在 submit 阶段直接拿 VertexConsumer 写顶点。
 *
 * <p>本类兼容旧 geckolib3 的 "getBuffer(RenderType) → 立即写顶点" 写法。{@link #getBuffer(RenderType)}
 * 返回一个延迟 sink，渲染结束时调用 {@link #flush(SubmitNodeCollector, PoseStack)}，通过
 * {@link SubmitNodeCollector#submitCustomGeometry} 在 vanilla 绘制阶段回放（顶点坐标在记录时已按
 * 当时 poseStack 变换完毕，回放时直接写原始值，不做二次变换——回调给的 pose 被忽略）。</p>
 *
 * <p><b>阶段②</b>：RenderType 获取点统一收口到 {@link YsmRenderTypes}（YSM 自定义 RenderPipeline），
 * 管线的 cull/混合/排序全部由 YSM 管线状态在 GPU 阶段承担；vanilla 类型（lines/outline/鹦鹉等
 * 原版子模型）照常透传，由 vanilla 管线处理。</p>
 *
 * <p><b>阶段③（GeoBufferSource 收敛）</b>：退役"逐顶点 op 流水 + switch 回放"的双份开销。
 * 经 javap 实证：submitCustomGeometry 的回调在 feature 渲染阶段（CustomFeatureRenderer.buildGroup）
 * 才执行，提取期拿不到 VertexConsumer（直接写入不可行）；且 26.3 BufferBuilder 按语义独立寻址
 * （顺序无关），并有 entityFormat 快路径——一次 {@code addVertex(x,y,z,color,u,v,uv1,uv2,nx,ny,nz)}
 * 单调用直写全部属性（vanilla ModelPart$Cube 同款）。因此：</p>
 * <ul>
 *   <li>实体顶点格式（Position/Color/UV0/UV1/UV2/Normal 六语义，即 DefaultVertexFormat.ENTITY，
 *       覆盖全部 YSM geo 模型与 vanilla 鞘翅/鹦鹉子模型）：走 {@link EntityVertexSink}——记录时
 *       按固定槽位直写 primitive 数组（零 dispatch、零装箱），回放时每顶点仅 1 次批量调用命中
 *       BufferBuilder 快路径；</li>
 *   <li>其它格式（lines 等罕见路径）：保留 op 流水兜底（{@link OpRecordingConsumer}）；</li>
 *   <li>同一 RenderType 的多次 getBuffer 合并为一个批次（等价 1.20.1 BufferSource 的复用语义，
 *       半透明 sortOnUpload 的逐 quad 排序在目标 draw 内进行，合并不改变排序语义）。</li>
 * </ul>
 */
public final class GeoBufferSource {

    private final List<Batch> batches = new ArrayList<>();
    private final Map<RenderType, Batch> batchByType = new LinkedHashMap<>();

    public VertexConsumer getBuffer(RenderType renderType) {
        Batch batch = this.batchByType.get(renderType);
        if (batch == null) {
            batch = new Batch(renderType, createSink(renderType));
            this.batchByType.put(renderType, batch);
            this.batches.add(batch);
        }
        return batch.sink();
    }

    private static VertexSink createSink(RenderType renderType) {
        return isEntityVertexFormat(renderType.format())
                ? new EntityVertexSink()
                : new OpRecordingConsumer();
    }

    /**
     * 实体顶点格式判定：六个语义齐备、且不含额外语义（UV3/LineWidth 等）。
     * 回放走 11 参批量 addVertex，语义与逐 setter 写入等价（见类注释的 javap 实证）。
     */
    private static boolean isEntityVertexFormat(com.mojang.renderpearl.api.vertex.VertexFormat format) {
        return format.getElements().size() == 6
                && format.contains(DefaultVertexFormat.POSITION_SEMANTIC_NAME)
                && format.contains(DefaultVertexFormat.COLOR_SEMANTIC_NAME)
                && format.contains(DefaultVertexFormat.UV0_SEMANTIC_NAME)
                && format.contains(DefaultVertexFormat.UV1_SEMANTIC_NAME)
                && format.contains(DefaultVertexFormat.UV2_SEMANTIC_NAME)
                && format.contains(DefaultVertexFormat.NORMAL_SEMANTIC_NAME);
    }

    public void flush(SubmitNodeCollector collector, PoseStack poseStack) {
        for (Batch batch : this.batches) {
            batch.sink().submit(collector, poseStack, batch.renderType());
        }
        this.batches.clear();
        this.batchByType.clear();
    }


    public boolean isEmpty() {
        return this.batches.isEmpty();
    }

    private interface VertexSink extends VertexConsumer {
        boolean isEmpty();

        void submit(SubmitNodeCollector collector, PoseStack poseStack, RenderType renderType);
    }

    private record Batch(RenderType renderType, VertexSink sink) {
    }

    /**
     * 实体格式专用 sink（阶段③核心）：记录时按固定槽位直写 primitive 数组——
     * 每顶点 8 float（px py pz u v nx ny nz）+ 3 int（argb、uv1 packed、uv2 packed），
     * 无 opcode dispatch、无装箱。回放每顶点仅 1 次 {@link VertexConsumer#addVertex(float, float, float,
     * int, float, float, int, int, float, float, float)}，命中 BufferBuilder 的 entityFormat 快路径
     * （vanilla ModelPart$Cube 同款调用）。
     *
     * <p>等价性说明（javap 实证）：setColor(r,g,b,a) 按字节序 R,G,B,A 写入 ≡ setColor(argb 打包)；
     * setUv1(u,v)/setUv2(u,v) 写两个 short ≡ setOverlay/setLight 的 putPackedUv
     * （低 16 位 = u，高 16 位 = v）。未设置的属性回放写 0，与 BufferBuilder 对未填充元素的
     * 零默认填充一致。</p>
     */
    private static final class EntityVertexSink implements VertexSink {

        private static final int FLOATS_PER_VERTEX = 8; // px py pz u v nx ny nz
        private static final int INTS_PER_VERTEX = 3;   // argb, uv1(packed), uv2(packed)

        private float[] floats = new float[FLOATS_PER_VERTEX * 1024];
        private int[] ints = new int[INTS_PER_VERTEX * 1024];
        private int vertexCount;

        @Override
        public boolean isEmpty() {
            return this.vertexCount == 0;
        }

        @Override
        public void submit(SubmitNodeCollector collector, PoseStack poseStack, RenderType renderType) {
            if (this.vertexCount == 0) {
                return;
            }
            // 顶点已在记录时按当时 poseStack 变换完毕；提交时忽略回调 pose，直接写原始坐标。
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> this.replay(buffer));
        }

        private void replay(VertexConsumer target) {
            int count = this.vertexCount;
            float[] f = this.floats;
            int[] i = this.ints;
            for (int v = 0; v < count; v++) {
                int fo = v * FLOATS_PER_VERTEX;
                int io = v * INTS_PER_VERTEX;
                // BufferBuilder entityFormat 快路径：一次调用直写 Position+Color+UV0+UV1+UV2+Normal
                target.addVertex(f[fo], f[fo + 1], f[fo + 2],
                        i[io], f[fo + 3], f[fo + 4],
                        i[io + 1], i[io + 2],
                        f[fo + 5], f[fo + 6], f[fo + 7]);
            }
        }

        private int growInts() {
            int vc = this.vertexCount;
            if ((vc + 1) * INTS_PER_VERTEX > this.ints.length) {
                this.ints = java.util.Arrays.copyOf(this.ints, this.ints.length * 2);
            }
            return vc * INTS_PER_VERTEX;
        }

        private int growFloats() {
            int vc = this.vertexCount;
            if ((vc + 1) * FLOATS_PER_VERTEX > this.floats.length) {
                this.floats = java.util.Arrays.copyOf(this.floats, this.floats.length * 2);
            }
            return vc * FLOATS_PER_VERTEX;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            int fo = this.growFloats();
            this.growInts();
            this.floats[fo] = x;
            this.floats[fo + 1] = y;
            this.floats[fo + 2] = z;
            // 未设置属性按 0 记录：回放显式写 0，与 BufferBuilder 未填充元素的零默认一致
            this.ints[this.vertexCount * INTS_PER_VERTEX] = 0;
            this.ints[this.vertexCount * INTS_PER_VERTEX + 1] = 0;
            this.ints[this.vertexCount * INTS_PER_VERTEX + 2] = 0;
            this.floats[fo + 3] = 0.0f;
            this.floats[fo + 4] = 0.0f;
            this.floats[fo + 5] = 0.0f;
            this.floats[fo + 6] = 0.0f;
            this.floats[fo + 7] = 0.0f;
            this.vertexCount = this.vertexCount + 1;
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            return this.setColor(((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF));
        }

        @Override
        public VertexConsumer setColor(int argb) {
            this.ints[(this.vertexCount - 1) * INTS_PER_VERTEX] = argb;
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            int fo = (this.vertexCount - 1) * FLOATS_PER_VERTEX;
            this.floats[fo + 3] = u;
            this.floats[fo + 4] = v;
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            // ≡ setOverlay：低 16 位 u、高 16 位 v（putPackedUv 字节序实证）
            return this.setOverlay(((v & 0xFFFF) << 16) | (u & 0xFFFF));
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            // ≡ setLight
            return this.setLight(((v & 0xFFFF) << 16) | (u & 0xFFFF));
        }

        @Override
        public VertexConsumer setOverlay(int packed) {
            this.ints[(this.vertexCount - 1) * INTS_PER_VERTEX + 1] = packed;
            return this;
        }

        @Override
        public VertexConsumer setLight(int packed) {
            this.ints[(this.vertexCount - 1) * INTS_PER_VERTEX + 2] = packed;
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            int fo = (this.vertexCount - 1) * FLOATS_PER_VERTEX;
            this.floats[fo + 5] = x;
            this.floats[fo + 6] = y;
            this.floats[fo + 7] = z;
            return this;
        }

        @Override
        public VertexConsumer setUv3(float u, float v) {
            // 实体格式无 UV3；行为与 BufferBuilder 一致（beginElement 返回 -1，写入被忽略）
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }
    }

    /**
     * 通用兜底 sink：仅用于非实体顶点格式（lines 等罕见路径，含 LineWidth/UV3 语义）。
     * 保留旧"逐 op 记录 → switch 回放"实现，顺序无关语义由 BufferBuilder 按语义寻址保证。
     */
    private static final class OpRecordingConsumer implements VertexSink {

        private static final int OP_VERTEX = 0;
        private static final int OP_COLOR_RGBA = 1;
        private static final int OP_COLOR_ARGB = 2;
        private static final int OP_UV = 3;
        private static final int OP_UV1 = 4;
        private static final int OP_UV2 = 5;
        private static final int OP_UV3 = 6;
        private static final int OP_NORMAL = 7;
        private static final int OP_LINE_WIDTH = 8;

        private final IntArrayList ops = new IntArrayList();
        private final FloatArrayList args = new FloatArrayList();

        private void op(int opcode, float... a) {
            this.ops.add(opcode);
            for (float v : a) {
                this.args.add(v);
            }
        }

        @Override
        public boolean isEmpty() {
            return this.ops.isEmpty();
        }

        @Override
        public void submit(SubmitNodeCollector collector, PoseStack poseStack, RenderType renderType) {
            if (this.ops.isEmpty()) {
                return;
            }
            // 顶点已在记录时按当时 poseStack 变换完毕；提交时忽略回调 pose，直接写原始坐标。
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> this.replay(buffer));
        }

        private void replay(VertexConsumer target) {
            float[] a = this.args.elements();
            int ai = 0;
            for (int i = 0; i < this.ops.size(); i++) {
                switch (this.ops.getInt(i)) {
                    case OP_VERTEX -> {
                        target.addVertex(a[ai], a[ai + 1], a[ai + 2]);
                        ai += 3;
                    }
                    case OP_COLOR_RGBA -> {
                        target.setColor(
                                Float.floatToRawIntBits(a[ai]),
                                Float.floatToRawIntBits(a[ai + 1]),
                                Float.floatToRawIntBits(a[ai + 2]),
                                Float.floatToRawIntBits(a[ai + 3]));
                        ai += 4;
                    }
                    case OP_COLOR_ARGB -> {
                        target.setColor(Float.floatToRawIntBits(a[ai]));
                        ai += 1;
                    }
                    case OP_UV -> {
                        target.setUv(a[ai], a[ai + 1]);
                        ai += 2;
                    }
                    case OP_UV1 -> {
                        target.setUv1((int) a[ai], (int) a[ai + 1]);
                        ai += 2;
                    }
                    case OP_UV2 -> {
                        target.setUv2((int) a[ai], (int) a[ai + 1]);
                        ai += 2;
                    }
                    case OP_UV3 -> {
                        target.setUv3(a[ai], a[ai + 1]);
                        ai += 2;
                    }
                    case OP_NORMAL -> {
                        target.setNormal(a[ai], a[ai + 1], a[ai + 2]);
                        ai += 3;
                    }
                    case OP_LINE_WIDTH -> {
                        target.setLineWidth(a[ai]);
                        ai += 1;
                    }
                }
            }
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.op(OP_VERTEX, x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            this.op(OP_COLOR_RGBA,
                    Float.intBitsToFloat(r), Float.intBitsToFloat(g),
                    Float.intBitsToFloat(b), Float.intBitsToFloat(a));
            return this;
        }

        @Override
        public VertexConsumer setColor(int argb) {
            this.op(OP_COLOR_ARGB, Float.intBitsToFloat(argb));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.op(OP_UV, u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            this.op(OP_UV1, u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            this.op(OP_UV2, u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv3(float u, float v) {
            this.op(OP_UV3, u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.op(OP_NORMAL, x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            this.op(OP_LINE_WIDTH, width);
            return this;
        }
    }
}
