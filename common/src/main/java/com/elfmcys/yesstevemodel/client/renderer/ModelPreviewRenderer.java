package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.client.bridge.RenderBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 26.3 GUI 内 3D 实体预览。
 *
 * <p>26.3 的 GUI 走 GuiRenderState 提取体系：实体预览通过
 * {@link net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState}（Pictures-in-Picture）
 * 提交，由 {@code GuiEntityRenderer} 在独立纹理上渲染，自带裁剪与 ENTITY_IN_UI 光照，
 * 无需旧的 RenderSystem model-view / scissor / MultiBufferSource。</p>
 *
 * <p><b>关键点</b>：渲染状态必须经 {@link EntityRenderDispatcher#extractEntity} 提取
 * （而不是 {@code InventoryScreen} 内部直接 {@code createRenderState} 的路径），
 * 这样 {@code EntityRenderDispatcherMixin} 的 state→entity 映射才会被填充，
 * PiP 渲染时 {@code dispatcher.submit} 内的 YSM 接管（CustomPlayerRenderer / GeoBufferSource）
 * 才会生效 —— 否则 GUI 里永远只画原版皮肤。</p>
 *
 * <p>TODO port: YSM 自定义 Geo 模型目前经 GeoBufferSource → SubmitNodeCollector 提交，
 * 在 PiP 纹理上渲染；rip.ysm.gpu GPU 加速路径（原生 SIMD 顶点构建）仍排除中，
 * 待其按 renderpearl 重写后再评估接入。</p>
 */
public final class ModelPreviewRenderer {

    private static boolean previewMode = false;

    private ModelPreviewRenderer() {
    }

    public static void setPreviewMode(boolean mode) {
        previewMode = mode;
        RenderBridge.preview = mode;
    }

    public static boolean isPreviewMode() {
        return previewMode;
    }

    /**
     * 26.3 port: 由 {@code LevelRendererMixin} 在 level 渲染帧开始/结束时调用（语义同 1.20.1）。
     * 原实现写本地 {@code isFirstPersonMode} 标志，相关读取点已迁移到
     * {@link RenderBridge}，这里同时回写桥位：
     * {@code firstPerson}（CameraUtil/YSMBinding 等查询用）与
     * {@code firstPersonOnRenderThread}（AnimatableEntity#processAnimation 的 isFirstPerson 路径，
     * 决定渲染时消费 EntityRenderCache 的异步动画结果，动画时间线推进依赖它）。
     */
    public static void setFirstPersonMode(boolean mode) {
        RenderBridge.firstPerson = mode;
        RenderBridge.firstPersonOnRenderThread = mode;
    }

    public static boolean isFirstPerson() {
        return RenderBridge.firstPerson;
    }

    /**
     * 鼠标跟随预览（原版 InventoryScreen.extractEntityInInventoryFollowsMouse 的等价实现，
     * 改为经 {@link EntityRenderDispatcher#extractEntity} 提取状态以保留 YSM 渲染接管）。
     *
     * @param scale   像素/方块（原版物品栏界面用 30）
     * @param offsetY 模型竖直方向偏移（模型单位，原版 0.0625）
     */
    public static void renderFollowsMouse(GuiGraphicsExtractor guiGraphics, int x0, int y0, int x1, int y1,
                                          float scale, float offsetY, float mouseX, float mouseY,
                                          LivingEntity entity, float partialTick) {
        float centerX = (x0 + x1) / 2.0f;
        float centerY = (y0 + y1) / 2.0f;
        float yaw = (float) Math.atan((centerX - mouseX) / 40.0f);
        float pitch = (float) Math.atan((centerY - mouseY) / 40.0f);
        EntityRenderState state = extractState(entity, partialTick);
        if (state == null) return;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 180.0f + yaw * 20.0f;
            living.yRot = yaw * 20.0f;
            if (living.pose != Pose.FALL_FLYING) {
                living.xRot = -pitch * 20.0f;
            } else {
                living.xRot = 0.0f;
            }
            normalizeScale(living);
        }
        Vector3f translation = new Vector3f(0.0f, state.boundingBoxHeight / 2.0f + offsetY, 0.0f);
        Quaternionf rotationZ = new Quaternionf().rotateZ(Mth.PI);
        Quaternionf rotationX = new Quaternionf().rotateX(pitch * 20.0f * 0.017453292519943295f);
        rotationZ.mul(rotationX);
        scale = fitScale(scale, state.boundingBoxWidth, state.boundingBoxHeight, x1 - x0, y1 - y0);
        submitEntity(guiGraphics, state, scale, translation, rotationZ, rotationX, x0, y0, x1, y1);
    }

    /**
     * 固定视角预览（模型/材质选择界面、模型按钮、纸娃娃等）。
     *
     * @param scale               像素/方块（1.20.1 各调用点的 zoom/scale 值可直接沿用）
     * @param cameraPitchDeg      相机俯仰角（度，1.20.1 预览惯用 -10）
     * @param bodyYawDeg          模型朝向（度，0 = 面向相机；1.20.1 预览惯用 20）
     * @param verticalPixelOffset 模型中心相对预览区中心的竖直偏移（像素，正值向上）
     */
    public static void renderFixed(GuiGraphicsExtractor guiGraphics, int x0, int y0, int x1, int y1,
                                   float scale, float cameraPitchDeg, float bodyYawDeg,
                                   float verticalPixelOffset, LivingEntity entity, float partialTick) {
        EntityRenderState state = extractState(entity, partialTick);
        if (state == null) return;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 180.0f + bodyYawDeg;
            living.yRot = bodyYawDeg;
            living.xRot = 0.0f;
            normalizeScale(living);
        }
        scale = fitScale(scale, state.boundingBoxWidth, state.boundingBoxHeight, x1 - x0, y1 - y0);
        Vector3f translation = new Vector3f(0.0f,
                state.boundingBoxHeight / 2.0f - verticalPixelOffset / scale, 0.0f);
        Quaternionf rotationZ = new Quaternionf().rotateZ(Mth.PI);
        Quaternionf rotationX = new Quaternionf().rotateX(cameraPitchDeg * 0.017453292519943295f);
        rotationZ.mul(rotationX);
        submitEntity(guiGraphics, state, scale, translation, rotationZ, rotationX, x0, y0, x1, y1);
    }

    /**
     * HUD 纸娃娃 / 额外玩家渲染（1.20.1 renderPlayerOverlay 的 26.3 版）。
     * ExtraPlayerOverlay / HudOverlay 仍在 build.gradle 排除列表中（依赖已删除的 GuiGraphics），
     * 恢复 HUD 时需将调用端改为 GuiGraphicsExtractor（fabric HudElement 体系）。
     */
    public static void renderPlayerOverlay(GuiGraphicsExtractor guiGraphics, LocalPlayer player,
                                           float posX, float posY, float scale, float yawOffset,
                                           float zLevel, float partialTick) {
        float bodyRot = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) + yawOffset;
        int halfW = Math.max(8, Math.round(scale));
        int halfH = Math.max(16, Math.round(scale * 2.0f));
        renderFixed(guiGraphics,
                Math.round(posX) - halfW, Math.round(posY) - halfH,
                Math.round(posX) + halfW, Math.round(posY) + halfH,
                scale, 0.0f, bodyRot - 180.0f, 0.0f, player, partialTick);
    }

    /**
     * {@link #renderFixed} 的 Animatable 重载：预览实体（PlayerPreviewEntity 等）是
     * LivingAnimatable 包装器，真正的 Entity 经 getEntity() 取出。
     */
    public static void renderFixed(GuiGraphicsExtractor guiGraphics, int x0, int y0, int x1, int y1,
                                   float scale, float cameraPitchDeg, float bodyYawDeg,
                                   float verticalPixelOffset,
                                   com.elfmcys.yesstevemodel.client.entity.LivingAnimatable<?> animatable,
                                   float partialTick) {
        if (!(animatable.getEntity() instanceof LivingEntity previewEntity)) {
            return;
        }
        renderFixed(guiGraphics, x0, y0, x1, y1, scale, cameraPitchDeg, bodyYawDeg,
                verticalPixelOffset, previewEntity, partialTick);
    }

    private static EntityRenderState extractState(LivingEntity entity, float partialTick) {        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null && minecraft.player == null) {
            return null;
        }
        return minecraft.getEntityRenderDispatcher().extractEntity(entity, partialTick);
    }

    private static void normalizeScale(LivingEntityRenderState living) {
        living.boundingBoxWidth /= living.scale;
        living.boundingBoxHeight /= living.scale;
        living.scale = 1.0f;
    }

    private static void submitEntity(GuiGraphicsExtractor guiGraphics, EntityRenderState state, float scale,
                                     Vector3f translation, Quaternionf rotation, Quaternionf cameraAngle,
                                     int x0, int y0, int x1, int y1) {
        guiGraphics.entity(state, scale, translation, rotation, cameraAngle, x0, y0, x1, y1);
    }

    /**
     * 自适应缩放：保证 YSM 模型完整落在预览区内。
     *
     * <p>1.20.1 直接以固定模型中心 + zoom 渲染，模型几何再大也只是被 scissor 裁掉边缘；
     * 26.3 PiP 以 bbox 中心 + 像素偏移定位，碰撞箱远小于 YSM 实际几何（长发/兽耳/裙摆/尾巴）
     * 的模型会溢出小尺寸预览区（模型按钮 52x70），表现为"模型嵌在灰底图里只见半个身体"。
     * 这里按 bbox（加余量系数，覆盖超出碰撞箱的装饰几何）收缩像素比例，保证全模可见。</p>
     */
    private static float fitScale(float scale, float bboxWidth, float bboxHeight, int regionW, int regionH) {
        // 1.35/1.5：YSM 模型装饰几何（头发/尾巴/武器等）普遍超出 vanilla 碰撞箱的经验余量
        float neededH = Math.max(bboxHeight, 1.0f) * scale * 1.35f;
        float neededW = Math.max(bboxWidth, 0.6f) * scale * 1.5f;
        float fit = Math.min(regionH / neededH, regionW / neededW);
        return fit < 1.0f ? scale * fit : scale;
    }
}
