package com.elfmcys.yesstevemodel.client.renderer.pip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

/**
 * YSM 自定义 PiP 实体渲染器（{@code GuiEntityRenderer} 的复刻，逐行同源）。
 *
 * <p>唯一的差别是"一个实例只服务一个预览 key"（由 {@link YsmPreviewRendererPool} 分配），
 * 因此每个预览拥有独立纹理，互不覆盖（根因见 {@link YsmPreviewRenderState} 类注释）。</p>
 */
public class YsmPreviewRenderer extends PictureInPictureRenderer<YsmPreviewRenderState> {

    /** 预览装饰与模型统一使用全亮光值（1.20.1 三处 renderSingleBlock/dispatcher.render 均传 15728880）。 */
    private static final int LIGHT_COORDS = 15728880;

    private static BlockModelResolver blockModelResolver;

    private final EntityRenderDispatcher entityRenderDispatcher;

    private boolean usedOnThisFrame;

    /** 本帧 GUI 缩放（{@link #prepare} 传入），用于把顶点包围盒从纹理像素换算回 GUI 像素。 */
    private int guiScale = 1;

    public YsmPreviewRenderer(EntityRenderDispatcher entityRenderDispatcher) {
        this.entityRenderDispatcher = entityRenderDispatcher;
    }

    @Override
    public void prepare(YsmPreviewRenderState renderState, GuiRenderState guiRenderState,
                        net.minecraft.client.renderer.feature.FeatureRenderDispatcher featureRenderDispatcher,
                        int guiScale) {
        this.guiScale = Math.max(1, guiScale);
        super.prepare(renderState, guiRenderState, featureRenderDispatcher, guiScale);
    }

    public boolean usedOnThisFrame() {
        return this.usedOnThisFrame;
    }

    public void resetUsedOnThisFrame() {
        this.usedOnThisFrame = false;
    }

    @Override
    public Class<YsmPreviewRenderState> getRenderStateClass() {
        return YsmPreviewRenderState.class;
    }

    @Override
    protected void renderToTexture(YsmPreviewRenderState renderState, PoseStack poseStack,
                                   SubmitNodeCollector submitNodeCollector) {
        Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        Quaternionfc overriddenCameraAngle = renderState.overrideCameraAngle();
        CameraRenderState cameraRenderState = new CameraRenderState();
        if (overriddenCameraAngle != null) {
            cameraRenderState.orientation = overriddenCameraAngle.conjugate(new Quaternionf()).rotateY((float) Math.PI);
        }
        // 动画测试界面的装饰（床 / 草地 / 载具）先于模型提交，与 1.20.1 renderEntityPreview 的顺序一致
        // （1.20.1 在两次 bufferSource.endBatch 之间先画装饰再画模型）。装饰使用**自己的基座变换**
        // （不含模型自身姿态，如 sleep 的 Ry(yaw-90)），见 YsmPreviewDecor。
        YsmPreviewDecor decor = renderState.decor();
        if (decor != null) {
            poseStack.pushPose();
            Vector3fc decorTranslation = decor.translation();
            poseStack.translate(decorTranslation.x(), decorTranslation.y(), decorTranslation.z());
            poseStack.rotate(decor.rotation());
            this.renderDecor(decor, cameraRenderState, poseStack, submitNodeCollector);
            poseStack.popPose();
        }
        Vector3fc translation = renderState.translation();
        // 纸娃娃的 region 自动收紧：采集本次提交的实体顶点包围盒（纹理像素 → 绝对 GUI 像素），
        // 供下一帧 planRegion 使用。非纸娃娃 key 不采集（见 PipPreviewBounds#markAutoFit）。
        PipPreviewBounds.beginCapture(renderState.key(), renderState.x0(), renderState.y0(), this.guiScale,
                (renderState.x1() - renderState.x0()) * this.guiScale,
                (renderState.y1() - renderState.y0()) * this.guiScale);
        try {
            poseStack.translate(translation.x(), translation.y(), translation.z());
            poseStack.rotate(renderState.rotation());
            this.entityRenderDispatcher.submit(renderState.renderState(), cameraRenderState,
                    0.0, 0.0, 0.0, poseStack, submitNodeCollector);
        } finally {
            PipPreviewBounds.endCapture();
        }
    }

    /**
     * 1.20.1 {@code renderBedPreview} / {@code renderGroundPreview} / {@code renderVehicleForAnimation} 的 26.3 版。
     *
     * <p>三者的变换链都从"模型基座旋转之后"开始（1.20.1 里它们各自 new 一个 PoseStack，但渲染时
     * 仍会乘上当时的 model-view，故与模型共享同一坐标系与 depth 方向）：</p>
     * <ul>
     *   <li>床：{@code Ry(yaw + 180) · T(-0.5, 0, 0.5)}（让床沿与躺下的模型对齐）；</li>
     *   <li>草地：{@code Ry(yaw) · T(-1.5, -1, -2.5)}，之后 3×3 草方块 + 草丛 + 郁金香按 1.20.1 的
     *       逐步 translate 排布；</li>
     *   <li>载具：{@code Ry(yaw)} 后按 {@code vehicleY} 下移车辆，使鞍座/座位对齐模型脚底。</li>
     * </ul>
     */
    private void renderDecor(YsmPreviewDecor decor, CameraRenderState cameraRenderState,
                             PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        float yaw = decor.yawDeg();
        if (decor.bed() != null) {
            poseStack.pushPose();
            poseStack.rotateDegrees(Axis.YP, yaw + 180.0f);
            poseStack.translate(-0.5f, 0.0f, 0.5f);
            submitBlock(poseStack, submitNodeCollector, decor.bed());
            poseStack.popPose();
        }
        if (decor.ground()) {
            poseStack.pushPose();
            poseStack.rotateDegrees(Axis.YP, yaw);
            poseStack.translate(-1.5f, -1.0f, -2.5f);
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    poseStack.translate(0.0f, 0.0f, 1.0f);
                    submitBlock(poseStack, submitNodeCollector, Blocks.GRASS_BLOCK.defaultBlockState());
                }
                poseStack.translate(1.0f, 0.0f, -3.0f);
            }
            poseStack.translate(-1.0f, 1.0f, 1.0f);
            submitBlock(poseStack, submitNodeCollector, Blocks.SHORT_GRASS.defaultBlockState());
            poseStack.translate(0.0f, 0.0f, 1.0f);
            submitBlock(poseStack, submitNodeCollector, Blocks.RED_TULIP.defaultBlockState());
            poseStack.popPose();
        }
        if (decor.vehicle() != null) {
            EntityRenderState vehicleState = this.entityRenderDispatcher.extractEntity(decor.vehicle(), 0.0f);
            poseStack.pushPose();
            poseStack.rotateDegrees(Axis.YP, yaw);
            this.entityRenderDispatcher.submit(vehicleState, cameraRenderState, 0.0, decor.vehicleY(), 0.0,
                    poseStack, submitNodeCollector);
            poseStack.popPose();
        }
    }

    /**
     * 单个方块的提交：26.3 用 {@code BlockModelResolver} 提取 {@link BlockModelRenderState}
     * （= 1.20.1 的 {@code BlockRenderer#renderSingleBlock}），再交给 {@code submitBlockModel} 进管线。
     * 方块模型集随资源重载更新，而 {@code Minecraft#getModelManager()} 实例不变，故解析器可长期缓存。
     */
    private static void submitBlock(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, BlockState blockState) {
        BlockModelResolver resolver = blockModelResolver;
        if (resolver == null) {
            resolver = new BlockModelResolver(Minecraft.getInstance().getModelManager());
            blockModelResolver = resolver;
        }
        BlockModelRenderState blockRenderState = new BlockModelRenderState();
        resolver.update(blockRenderState, blockState, BlockDisplayContext.create());
        blockRenderState.submit(poseStack, submitNodeCollector, LIGHT_COORDS, OverlayTexture.NO_OVERLAY, 0);
    }

    /**
     * 与 vanilla {@code OversizedItemRenderer} 同款：blit 时标记本帧用过，
     * 供 {@link YsmPreviewRendererPool} 在帧末回收不再出现的预览（连同其纹理）。
     */
    @Override
    public void blitTexture(YsmPreviewRenderState renderState, GuiRenderState guiRenderState) {
        super.blitTexture(renderState, guiRenderState);
        this.usedOnThisFrame = true;
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "ysm_entity";
    }
}
