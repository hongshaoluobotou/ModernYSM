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

    private ModelPreviewRenderer() {
    }

    public static void setPreviewMode(boolean mode) {
        previewMode = mode;
    }

    public static boolean isPreviewMode() {
        return previewMode;
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
