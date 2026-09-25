package com.elfmcys.yesstevemodel.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.ArrayList;
import java.util.List;

/**
 * 26.3 移植：客户端渲染已改为 EntityRenderState + submit/GpuBuffer（renderpearl）体系，
 * {@code MultiBufferSource}/{@code BufferSource} 被移除，无法再在 submit 阶段直接拿 VertexConsumer 写顶点。
 *
 * <p>本类兼容旧 geckolib3 的 "getBuffer(RenderType) → 立即写顶点" 写法：
 * {@link #getBuffer(RenderType)} 返回一个 {@link RecordingConsumer}，把逐顶点调用记录下来；
 * 渲染结束时调用 {@link #flush(SubmitNodeCollector, PoseStack)}，通过
 * {@link SubmitNodeCollector#submitCustomGeometry} 把记录的顶点在 vanilla 绘制阶段回放到真正的
 * VertexConsumer 上（顶点坐标在记录时已按当时 poseStack 变换完毕，回放时直接写原始值，不做二次变换）。</p>
 *
 * <p>// TODO port: gpu path — rip.ysm.gpu 的 GpuRenderPath 恢复后，可在此处旁路直接提交 GPU 缓冲。</p>
 */
public final class GeoBufferSource {

    private final List<Batch> batches = new ArrayList<>();

    public VertexConsumer getBuffer(RenderType renderType) {
        RecordingConsumer consumer = new RecordingConsumer();
        this.batches.add(new Batch(renderType, consumer));
        return consumer;
    }

    public void flush(SubmitNodeCollector collector, PoseStack poseStack) {
        for (Batch batch : this.batches) {
            batch.consumer().submit(collector, poseStack, batch.renderType());
        }
        this.batches.clear();
    }

    public boolean isEmpty() {
        return this.batches.isEmpty();
    }

    private record Batch(RenderType renderType, RecordingConsumer consumer) {
    }

    private static final class RecordingConsumer implements VertexConsumer {

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

        private void submit(SubmitNodeCollector collector, PoseStack poseStack, RenderType renderType) {
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
