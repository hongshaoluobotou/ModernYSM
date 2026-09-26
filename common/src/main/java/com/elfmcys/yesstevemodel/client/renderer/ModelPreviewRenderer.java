package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.client.bridge.RenderBridge;
import com.elfmcys.yesstevemodel.client.entity.PlayerPreviewEntity;
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

    /**
     * 26.3 port: GUI 预览的模型朝向。1.20.1 在 renderEntityPreview/renderLivingEntityPreview 中
     * 直接改写预览实体的 yBodyRot/yRot/yHeadRot 再渲染（geo 渲染路径经
     * AnimatableEntity.processAnimationImpl → modelData.lerpBodyRot → setupRotations 消费实体字段，
     * 渲染状态上的 bodyRot 对 YSM geo 模型无效）。PiP 体系下提取与提交分处一帧的两端，
     * 这里按 UUID 暂存预览朝向，由 CustomPlayerRenderer#renderPlayer 在 submit 时同步改写/还原；
     * 仅对 DummyPlayer 预览实体生效（世界内永不渲染 DummyPlayer），故无需按帧清理。
     */
    public static final java.util.concurrent.ConcurrentHashMap<java.util.UUID, Float> PREVIEW_YAW =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 26.3 port: 背包（InventoryScreen）玩家预览的朝向桥接。
     *
     * <p>1.20.1 的 {@code InventoryScreen.renderEntityInInventoryFollowsMouse} 直接改写预览实体
     * 的 yBodyRot/yRot/xRot（geo 渲染路径自然吃到）；26.3 vanilla 改的是
     * {@link LivingEntityRenderState} 的 bodyRot/yRot/xRot 字段，geo 路径读不到。
     * {@link com.elfmcys.yesstevemodel.mixin.client.InventoryScreenMixin} 在 vanilla 设置完
     * 旋转后按<b>渲染状态对象</b>（弱键，每帧新建）暂存此处；CustomPlayerRenderer#renderPlayer
     * 在 submit 时命中该状态则同步改写实体旋转、渲染后还原并移除条目。
     * 以 state 身份为键（而非 UUID）：背包预览实体是真实 LocalPlayer，同一玩家同帧还会被
     * 世界渲染路径（另一个 state）使用，按 UUID 存会污染世界内旋转。</p>
     */
    public static final java.util.concurrent.ConcurrentHashMap<EntityRenderState, float[]> INVENTORY_PREVIEW_ROT =
            new java.util.concurrent.ConcurrentHashMap<>();

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
     * 鼠标跟随预览（PlayerModelScreen 左侧主预览）。
     *
     * <p><b>与 1.20.1 逐值对照</b>（2026.09.26）：1.20.1 走原版
     * {@code InventoryScreen.renderEntityInInventoryFollowsMouse(g, x, y, size, relX, relY, entity)}
     * ——其内部为 {@code translate(x, y, 50) · scaling(size, size, -size) · Rz(180)·Rx}，
     * 即<b>模型脚底锚定在像素 (x, y)</b>、scale 为像素/块。YSM 1.20.1 调用点：
     * 区域 (guiLeft+5, guiTop+29)~(guiLeft+130, guiTop+200)、anchor (guiLeft+67, guiTop+190)、
     * size=70、鼠标跟随参考点 (guiLeft+67, guiTop+85)。</p>
     *
     * <p>26.3 PiP 语义换算：{@code GuiEntityRenderer.renderToTexture} 的完整变换为
     * {@code T(w/2, h/2) · S(s, s, -s) · T(translation) · R · v}（s = guiScale·scale；
     * {@code PictureInPictureRenderer.prepare} 先 translate(w/2, h/2)，其中
     * {@code getTranslateY(h, guiScale)} 返回<b>高度/2</b>——竖直锚点在区域顶下方
     * (y1-y0)/2 像素处（区域竖直中心）；
     * feet 在 Rz(180) 后映射到 translation.y，故 translation.y = (anchorY - 竖直锚点)/scale，
     * 正值向下）。yaw/pitch 用 1.20.1 的跟随参考点而非区域中心（1.20.1 原版函数直接收相对量）。</p>
     */
    public static void renderFollowsMouse(GuiGraphicsExtractor guiGraphics, int x0, int y0, int x1, int y1,
                                          float scale, float followCenterX, float followCenterY, float feetAnchorY,
                                          float mouseX, float mouseY,
                                          LivingEntity entity, float partialTick) {
        float yaw = (float) Math.atan((followCenterX - mouseX) / 40.0f);
        float pitch = (float) Math.atan((followCenterY - mouseY) / 40.0f);
        EntityRenderState state = extractState(entity, partialTick);
        if (state == null) return;
        float stateBodyRot = 180.0f + yaw * 20.0f;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = stateBodyRot;
            living.yRot = yaw * 20.0f;
            if (living.pose != Pose.FALL_FLYING) {
                living.xRot = -pitch * 20.0f;
            } else {
                living.xRot = 0.0f;
            }
            normalizeScale(living);
        }
        // 26.3 port: geo 渲染路径读实体旋转（见 PREVIEW_YAW 注释），暂存预览朝向供 renderPlayer 使用
        if (entity instanceof net.minecraft.world.entity.player.Player previewPlayer && PlayerPreviewEntity.isPreviewPlayer(previewPlayer)) {
            PREVIEW_YAW.put(entity.getUUID(), stateBodyRot);
        }
        // 竖直锚点 = 区域竖直中心 y0 + (y1-y0)/2（PictureInPictureRenderer.prepare 的
        // getTranslateY(h, guiScale) 返回高度/2；此前误用宽度/2 导致模型整体下移
        // (h-w)/2 像素——相框错位 + 底部被裁的根因）
        Vector3f translation = new Vector3f(0.0f, (feetAnchorY - (y0 + (y1 - y0) / 2.0f)) / scale, 0.0f);
        Quaternionf rotationZ = new Quaternionf().rotateZ(Mth.PI);
        Quaternionf rotationX = new Quaternionf().rotateX(pitch * 20.0f * 0.017453292519943295f);
        rotationZ.mul(rotationX);
        // 26.3 vanilla（InventoryScreen.extractEntityInInventoryFollowsMouse）：override 只传
        // Rx(pitch)（GuiEntityRenderer 内部 conjugalte().rotateY(PI) 自补 Rz(180)），
        // 传合成 Rz·Rx 会使相机多转 180° 俯仰
        guiGraphics.entity(state, scale, translation, rotationZ, rotationX, x0, y0, x1, y1);
    }

    /**
     * 固定视角预览（模型/材质选择界面、模型按钮）。
     *
     * <p><b>与 1.20.1 逐值对照</b>（2026.09.26）：1.20.1 {@code renderLivingEntityPreview(x, y, scale, ...)}
     * 的模型视图变换为 {@code T(x, y, 1050) · S(1,1,-1) · T(0,0,1000) · S(scale) · Rz(180)·Rx(-10)}，
     * 即<b>模型脚底锚定在像素 (x, y)</b>、scale 为像素/块的<b>固定值</b>（无任何按 bbox 自适应），
     * 超出 scissor 的部分直接裁掉。</p>
     *
     * <p>26.3 PiP 换算：feet 经 Rz(180) 映射到 translation.y；PiP 竖直锚点在
     * {@code y0 + (y1-y0)/2}（vanilla getTranslateY 用高度/2）、水平锚点在区域中心。
     * 故 translation.x = (anchorX - 水平锚点)/scale、translation.y = (anchorY - 竖直锚点)/scale。
     * {@code bodyYawDeg}: 1.20.1 实体 yBodyRot=200 → 传 20（内部 state.bodyRot = 180+deg）；
     * disablePreviewRotation 时 1.20.1 额外 translate(0, 5.5, 1000)（旋转前的屏幕空间下移 5.5px），
     * 由调用点折入 anchorY。</p>
     *
     * @param cameraPitchDeg 相机俯仰角（度，1.20.1 预览惯用 -10；disablePreviewRotation 时 0）
     * @param bodyYawDeg     模型朝向（度，0 = 面向相机；1.20.1 预览惯用 20）
     * @param anchorX        模型脚底锚点 x（GUI 像素，1.20.1 renderLivingEntityPreview 的 x）
     * @param anchorY        模型脚底锚点 y（GUI 像素，1.20.1 renderLivingEntityPreview 的 y）
     */
    public static void renderFixed(GuiGraphicsExtractor guiGraphics, int x0, int y0, int x1, int y1,
                                   float scale, float cameraPitchDeg, float bodyYawDeg,
                                   float anchorX, float anchorY, LivingEntity entity, float partialTick) {
        submitFixed(guiGraphics, x0, y0, x1, y1, scale, cameraPitchDeg, bodyYawDeg, anchorX, anchorY,
                entity, partialTick, null);
    }

    /**
     * {@link #renderFixed} 的 Animatable 重载：预览实体（PlayerPreviewEntity 等）是
     * LivingAnimatable 包装器，真正的 Entity 经 getEntity() 取出。
     */
    public static void renderFixed(GuiGraphicsExtractor guiGraphics, int x0, int y0, int x1, int y1,
                                   float scale, float cameraPitchDeg, float bodyYawDeg,
                                   float anchorX, float anchorY,
                                   com.elfmcys.yesstevemodel.client.entity.LivingAnimatable<?> animatable,
                                   float partialTick) {
        if (!(animatable.getEntity() instanceof LivingEntity previewEntity)) {
            return;
        }
        renderFixed(guiGraphics, x0, y0, x1, y1, scale, cameraPitchDeg, bodyYawDeg,
                anchorX, anchorY, previewEntity, partialTick);
    }

    /**
     * 带姿态偏移的固定预览（ModernPlayerTextureScreen 动画测试）。
     *
     * <p><b>与 1.20.1 逐值对照</b>（2026.09.26）：1.20.1 {@code renderEntityPreview} 与
     * {@code renderPlayerForSettings} 同为 feet 锚定，但 poseStack 链为
     * {@code T(cx, cy) · S(1,1,-1) · T(0,0,1000) · S(zoom) · T(0, 0.8, 0) · Rz(180)·Rx(-10+pitch)}，
     * 其中 {@code T(0,0.8,0)} 位于 S(zoom) 与旋转之间（屏幕空间下移 0.8·zoom 像素）→ 折入 anchorY =
     * cy + 0.8·zoom。另有按当前动画的条件偏移/姿态（post-multiplied = 模型空间预旋转）：</p>
     * <ul>
     *   <li>sit: 模型空间 (0,-0.5,0)；ride: (0,0.85,0)；ride_pig: (0,0.3125,0)；boat: (0,-0.45,0)
     *       —— 预旋转偏移经 Rz(180)·Rx 折进 PiP translation；</li>
     *   <li>swim/swim_stand → Pose.SWIMMING；sneak/sneaking → Pose.CROUCHING（提取前临时设置实体姿态）；</li>
     *   <li>sleep 的 yaw-90 旋转 + (0.5,0.5625,0) 平移 + 床/地面/坐骑方块渲染 26.3 暂未复刻（TODO）。</li>
     * </ul>
     */
    public static void renderAnimationPreview(GuiGraphicsExtractor guiGraphics, int x0, int y0, int x1, int y1,
                                              float scale, float cameraPitchDeg, float bodyYawDeg,
                                              float anchorX, float anchorY,
                                              com.elfmcys.yesstevemodel.client.entity.LivingAnimatable<?> animatable,
                                              float partialTick) {
        if (!(animatable.getEntity() instanceof LivingEntity previewEntity)) {
            return;
        }
        var animationTracker = ((com.elfmcys.yesstevemodel.client.entity.IPreviewAnimatable) animatable).getAnimationStateMachine();
        float modelYOffset = 0.0f;
        Pose poseOverride = null;
        if (animationTracker.isCurrentAnimation("sit")) {
            modelYOffset = -0.5f;
        } else if (animationTracker.isCurrentAnimation("ride")) {
            modelYOffset = 0.85f;
        } else if (animationTracker.isCurrentAnimation("ride_pig")) {
            modelYOffset = 0.3125f;
        } else if (animationTracker.isCurrentAnimation("boat")) {
            modelYOffset = -0.45f;
        }
        if (animationTracker.isCurrentAnimation("swim") || animationTracker.isCurrentAnimation("swim_stand")) {
            poseOverride = Pose.SWIMMING;
        } else if (animationTracker.isCurrentAnimation("sneak") || animationTracker.isCurrentAnimation("sneaking")) {
            poseOverride = Pose.CROUCHING;
        }
        // TODO port 26.3: sleep 动画的 yaw-90 旋转、(0.5,0.5625,0) 平移、床与地面/坐骑方块预览未复刻
        Pose oldPose = previewEntity.getPose();
        if (poseOverride != null) {
            previewEntity.setPose(poseOverride);
        }
        try {
            Quaternionf rotation = fixedRotation(cameraPitchDeg);
            Vector3f modelOffset = modelYOffset == 0.0f ? null : new Vector3f(0.0f, modelYOffset, 0.0f);
            submitFixed(guiGraphics, x0, y0, x1, y1, scale, cameraPitchDeg, bodyYawDeg, anchorX, anchorY,
                    previewEntity, partialTick, modelOffset);
        } finally {
            previewEntity.setPose(oldPose);
        }
    }

    /**
     * HUD 纸娃娃 / 额外玩家渲染（1.20.1 renderPlayerOverlay 的 26.3 版）。
     *
     * <p>1.20.1：模型视图 {@code T(x + s·0.5, y + s·2.0) · S(1,1,-1) · S(scale) · Rz(180.1)·Ry(bodyRot-180)}，
     * 模型脚底锚定 (x + s·0.5, y + s·2.0)，Rz(180.1)+Ry(bodyRot-180) 等价于
     * state.bodyRot = bodyRot（renderFixed 的 180+deg 语义 → deg = bodyRot-180，此处 180.1 的
     * 0.1° 补偿忽略）。ExtraPlayerOverlay / HudOverlay 仍在 build.gradle 排除列表中
     * （依赖已删除的 GuiGraphics），恢复 HUD 时需将调用端改为 GuiGraphicsExtractor（fabric HudElement 体系）。</p>
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
                scale, 0.0f, bodyRot - 180.0f,
                posX + scale * 0.5f, posY + scale * 2.0f, player, partialTick);
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

    /** Rz(180)·Rx(cameraPitchDeg)（1.20.1 预览的 rotationZ.mul(rotationX)）。 */
    private static Quaternionf fixedRotation(float cameraPitchDeg) {
        Quaternionf rotationZ = new Quaternionf().rotateZ(Mth.PI);
        rotationZ.mul(new Quaternionf().rotateX(cameraPitchDeg * 0.017453292519943295f));
        return rotationZ;
    }

    private static void submitFixed(GuiGraphicsExtractor guiGraphics, int x0, int y0, int x1, int y1,
                                    float scale, float cameraPitchDeg, float bodyYawDeg,
                                    float anchorX, float anchorY, LivingEntity entity, float partialTick,
                                    Vector3f preRotationModelOffset) {
        EntityRenderState state = extractState(entity, partialTick);
        if (state == null) return;
        float stateBodyRot = 180.0f + bodyYawDeg;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = stateBodyRot;
            living.yRot = bodyYawDeg;
            living.xRot = 0.0f;
            normalizeScale(living);
        }
        // 26.3 port: geo 渲染路径读实体旋转（见 PREVIEW_YAW 注释），暂存预览朝向供 renderPlayer 使用
        if (entity instanceof net.minecraft.world.entity.player.Player previewPlayer && PlayerPreviewEntity.isPreviewPlayer(previewPlayer)) {
            PREVIEW_YAW.put(entity.getUUID(), stateBodyRot);
        } else if (entity instanceof net.minecraft.world.entity.player.Player) {
            // 26.3 port (ExtraPlayerRenderScreen 右键拖拽旋转修复)：renderPlayerOverlay 传入的是
            // <b>真实 LocalPlayer</b>（额外玩家渲染配置界面的纸娃娃），geo 路径只认实体字段，
            // 而真实玩家同帧还会被世界渲染路径用另一个 state 渲染——不能按 UUID 挂（会污染世界内
            // 旋转），复用 INVENTORY_PREVIEW_ROT 的 state 弱键语义：本 state 只在本次 PiP 提交中
            // 被 CustomPlayerRenderer#renderPlayer 消费（用后即 remove），世界渲染路径的 state 不命中。
            INVENTORY_PREVIEW_ROT.put(state, new float[]{stateBodyRot, stateBodyRot, 0.0f});
        }
        Quaternionf rotation = fixedRotation(cameraPitchDeg);
        // 1.20.1 feet 锚定 → PiP translation：竖直锚点 = 区域竖直中心 y0 + (y1-y0)/2
        // （PictureInPictureRenderer.prepare 的 getTranslateY(h, guiScale) 返回<b>高度/2</b>；
        // 此前误用宽度/2 导致模型整体下移 (h-w)/2 像素——2D 相框错位 + 底部被裁的根因），
        // 水平锚点 = 区域中心；feet 经 Rz(180) 落在 translation.y，故按像素差 / scale 折算
        Vector3f translation = new Vector3f(
                (anchorX - (x0 + x1) / 2.0f) / scale,
                (anchorY - (y0 + (y1 - y0) / 2.0f)) / scale,
                0.0f);
        // 预旋转模型空间偏移（sit/ride 等）折进 translation：Δ = R · offset
        if (preRotationModelOffset != null) {
            translation.add(preRotationModelOffset.rotate(rotation));
        }
        // override 只传 Rx 部分（同 vanilla InventoryScreen 模式），rotation 含 Rz(180)
        guiGraphics.entity(state, scale, translation, rotation,
                new Quaternionf().rotateX(cameraPitchDeg * 0.017453292519943295f), x0, y0, x1, y1);
    }
}
