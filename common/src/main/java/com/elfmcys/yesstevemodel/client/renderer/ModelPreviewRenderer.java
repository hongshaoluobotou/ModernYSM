package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.geckolib3.geo.GeoReplacedEntityRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

/**
 * 26.3 port: 原实现依赖已删除的 MultiBufferSource.BufferSource / Tesselator 立即渲染管线，
 * 在 26.3 的 GuiRenderState（submit/extract 体系）下无法直接工作。
 * 本文件暂以空实现维持 GUI 编译与调用点；待 GUI 自定义几何提交路径
 * （GeoBufferSource → GuiRenderState / rip.ysm.gpu 重写）完成后恢复完整 3D 预览。
 * 完整历史实现见 git 历史（port/26.3 分支 GUI 恢复提交之前的版本）。
 */
public final class ModelPreviewRenderer {

    private static boolean previewMode = false;

    private static boolean firstPersonMode = false;

    private ModelPreviewRenderer() {
    }

    public static void setPreviewMode(boolean mode) {
        previewMode = mode;
    }

    public static boolean isPreviewMode() {
        return previewMode;
    }

    /**
     * 26.3 port: 由 {@code WorldRendererMixin} 在 level 渲染帧开始/结束时调用（语义同 1.20.1）。
     * 原实现写本地 {@code isFirstPersonMode} 标志，渲染层被排除期间相关读取点已迁移到
     * {@link com.elfmcys.yesstevemodel.client.bridge.RenderBridge}，这里同时回写两个桥位：
     * {@code firstPerson}（CameraUtil/YSMBinding 等查询用）与
     * {@code firstPersonOnRenderThread}（AnimatableEntity#processAnimation 的 isFirstPerson 路径，
     * 决定渲染时消费 EntityRenderCache 的异步动画结果，动画时间线推进依赖它）。
     */
    public static void setFirstPersonMode(boolean mode) {
        firstPersonMode = mode;
        com.elfmcys.yesstevemodel.client.bridge.RenderBridge.firstPerson = mode;
        com.elfmcys.yesstevemodel.client.bridge.RenderBridge.firstPersonOnRenderThread = mode;
    }

    public static boolean isFirstPerson() {
        return firstPersonMode;
    }

    public static void renderEntityPreview(float x, float y, float scale, float partialTick,
                                           com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity<?> animatableEntity,
                                           GeoReplacedEntityRenderer renderer, boolean renderGround) {
        // TODO port: 3D 预览待 GUI 自定义几何提交路径完成后恢复
    }

    public static void renderLivingEntityPreview(float x, float y, float scale, float partialTick,
                                                 com.elfmcys.yesstevemodel.client.entity.PlayerPreviewEntity holder,
                                                 GeoReplacedEntityRenderer renderer, boolean disablePreviewRotation, boolean renderGround) {
        // TODO port: 3D 预览待 GUI 自定义几何提交路径完成后恢复
    }

    public static void renderPlayerOverlay(GuiGraphicsExtractor guiGraphics, LocalPlayer player,
                                           float posX, float posY, float scale, float yawOffset, float zLevel, float partialTick) {
        // TODO port: 3D 预览待 GUI 自定义几何提交路径完成后恢复
    }
}
