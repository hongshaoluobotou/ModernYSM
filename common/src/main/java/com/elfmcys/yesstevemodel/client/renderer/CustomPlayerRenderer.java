package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import rip.ysm.compat.touhoulittlemaid.TouhouLittleMaidCompat;
import rip.ysm.compat.gun.swarfare.SWarfareCompat;
import com.elfmcys.yesstevemodel.client.bridge.RenderBridge;
import com.elfmcys.yesstevemodel.client.entity.PlayerPreviewEntity;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomPlayerArmorLayer;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomPlayerElytraLayer;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomPlayerItemInHandLayer;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomPlayerParrotLayer;
import com.elfmcys.yesstevemodel.event.api.SpecialPlayerRenderEvent;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoReplacedEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.NotNull;

public class CustomPlayerRenderer extends GeoReplacedEntityRenderer<Player, CustomPlayerEntity> {

    private Identifier currentTexture;

    public CustomPlayerRenderer(EntityRendererProvider.Context context) {
        super(context);
        addLayerRenderer(new CustomPlayerItemInHandLayer(context));
        addLayerRenderer(new CustomPlayerElytraLayer(context));
        addLayerRenderer(new CustomPlayerParrotLayer(context));
        addLayerRenderer(new CustomPlayerArmorLayer(context));
    }

    /**
     * 26.3 port: 原 render(Player, ...)（MultiBufferSource 立即渲染）改为 submit 体系入口，
     * 由 mixin 在 EntityRenderDispatcher.submit 处调用；返回 true 表示已接管本次玩家渲染。
     */
    public boolean renderPlayer(Player player, float partialTick, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, AvatarRenderState vanillaState) {
        PlayerCapability capability;
        if (SWarfareCompat.isPlayerAiming(player) || (capability = PlayerCapability.get(player).orElse(null)) == null) {
            return false;
        }
        capability.tickModel();
        SpecialPlayerRenderEvent renderEvent = new SpecialPlayerRenderEvent(player, capability, capability.getModelId());
        this.currentTexture = renderEvent.getTextureLocation();
        if (!SpecialPlayerRenderEvent.post(renderEvent)) {
            return false;
        }
        int packedLight = this.entityRenderDispatcher.getPackedLightCoords(player, partialTick);
        setCurrentCollector(submitNodeCollector);
        extractLayerRenderState(player, partialTick);
        GeoBufferSource bufferSource = new GeoBufferSource();
        setCurrentRTB(bufferSource);
        // 26.3 port: GUI 预览朝向 —— geo 渲染路径经 processAnimationImpl 读实体 yBodyRot/yRot/yHeadRot，
        // PiP 提取阶段改渲染状态无效（1.20.1 由 renderEntityPreview 直接改实体字段）。
        // 这里在 submit 内同步改写并在渲染后还原，保证世界内渲染（本帧 level 阶段已结束/下一帧
        // WorldRendererMixin 会清空 PREVIEW_YAW）不受影响。
        Float previewYaw = ModelPreviewRenderer.PREVIEW_YAW.get(player.getUUID());
        // 26.3 port: 背包（InventoryScreen）预览旋转桥接——vanilla 26.3 把鼠标跟随旋转设在
        // LivingEntityRenderState 上（1.20.1 改实体字段），按 state 身份取回并改写实体旋转。
        // 每帧的 PiP state 只渲染一次，用后即移除；YSM 未接管（无 capability）时残留条目
        // 随 state 弱键回收，无泄漏。
        float[] inventoryRot = vanillaState == null ? null : ModelPreviewRenderer.INVENTORY_PREVIEW_ROT.remove(vanillaState);
        // 26.3 port（setPreviewMode 语义核对结论）：1.20.1 的 isPreviewMode/isExtraPlayerMode 是
        // **渲染作用域标志**——renderEntityPreview/renderLivingEntityPreview（含 renderPlayerOverlay）
        // 进入时置位、退出时复位，geo 渲染在作用域内立即执行，故消费点（GeoReplacedEntityRenderer
        // fireRenderEvents、CameraUtil.isThirdPersonModel→molang rendering_in_inventory、
        // YSMBinding rendering_in_paperdoll、NativeModelRenderer isPreview、GeoEntity 物理管理器分流）
        // 在预览 geo 渲染期间读到 true。26.3 PiP 体系把"提取"与"geo 渲染"拆到帧的两端，
        // 不能再在提取处包渲染作用域；预览实体的 geo 渲染统一收敛在本方法内，故在此处按
        // 预览/纸娃娃身份置位 RenderBridge（用 finally 复原）：
        //   - PREVIEW_YAW 命中（DummyPlayer 预览：模型按钮/材质格/主预览/设置界面）→ preview=true；
        //   - INVENTORY_PREVIEW_ROT 命中（真实玩家纸娃娃：P 键配置界面）→ extraPlayer=true。
        // 移植期该标志从未被置位 → rendering_in_paperdoll 恒 false、预览期间误发渲染事件。
        boolean isPaperdoll = inventoryRot != null;
        boolean isPreviewPlayer = previewYaw != null && PlayerPreviewEntity.isPreviewPlayer(player);
        boolean oldPreview = RenderBridge.preview;
        boolean oldExtraPlayer = RenderBridge.extraPlayer;
        if (isPreviewPlayer) {
            RenderBridge.preview = true;
        }
        if (isPaperdoll) {
            RenderBridge.extraPlayer = true;
        }
        float oldBodyRot = 0.0f, oldBodyRotO = 0.0f, oldYRot = 0.0f, oldYRotO = 0.0f,
                oldXRot = 0.0f, oldXRotO = 0.0f, oldHeadRot = 0.0f, oldHeadRotO = 0.0f;
        boolean previewRotated = false;
        if ((previewYaw != null && PlayerPreviewEntity.isPreviewPlayer(player)) || inventoryRot != null) {
            float bodyRot = inventoryRot != null ? inventoryRot[0] : previewYaw;
            float yRot = inventoryRot != null ? inventoryRot[1] : previewYaw;
            float xRot = inventoryRot != null ? inventoryRot[2] : 0.0f;
            previewRotated = true;
            oldBodyRot = player.yBodyRot;
            oldBodyRotO = player.yBodyRotO;
            oldYRot = player.getYRot();
            oldYRotO = player.yRotO;
            oldXRot = player.getXRot();
            oldXRotO = player.xRotO;
            oldHeadRot = player.yHeadRot;
            oldHeadRotO = player.yHeadRotO;
            player.yBodyRot = bodyRot;
            player.yBodyRotO = bodyRot;
            player.setYRot(yRot);
            player.yRotO = yRot;
            player.setXRot(xRot);
            player.xRotO = xRot;
            player.yHeadRot = bodyRot;
            player.yHeadRotO = bodyRot;
        }
        try {
            renderEntityWithTexture(capability, renderEvent.getTextureLocation(), player.getYRot(), partialTick, poseStack, bufferSource, packedLight);
        } finally {
            RenderBridge.preview = oldPreview;
            RenderBridge.extraPlayer = oldExtraPlayer;
            if (previewRotated) {
                player.yBodyRot = oldBodyRot;
                player.yBodyRotO = oldBodyRotO;
                player.setYRot(oldYRot);
                player.yRotO = oldYRotO;
                player.setXRot(oldXRot);
                player.xRotO = oldXRotO;
                player.yHeadRot = oldHeadRot;
                player.yHeadRotO = oldHeadRotO;
            }
        }
        bufferSource.flush(submitNodeCollector, poseStack);
        return true;
    }

    public boolean shouldShowName(Player entity) {
        Minecraft minecraft;
        LocalPlayer localPlayer;
        double dDistanceToSqr = this.entityRenderDispatcher.distanceToSqr(entity);
        float nameRenderDistance = entity.isDiscrete() ? 32.0f : 64.0f;
        if (dDistanceToSqr >= nameRenderDistance * nameRenderDistance || (localPlayer = (minecraft = Minecraft.getInstance()).player) == null) {
            return false;
        }
        boolean isVisible = !entity.isInvisibleTo(localPlayer);
        if (entity != localPlayer) {
            Team team = entity.getTeam();
            Team team2 = localPlayer.getTeam();
            if (team != null) {
                switch (team.getNameTagVisibility()) {
                    case ALWAYS:
                        return isVisible;
                    case NEVER:
                        return false;
                    case HIDE_FOR_OTHER_TEAMS:
                        return team2 == null ? isVisible : team.isAlliedTo(team2) && (team.canSeeFriendlyInvisibles() || isVisible);
                    case HIDE_FOR_OWN_TEAM:
                        return team2 == null ? isVisible : !team.isAlliedTo(team2) && isVisible;
                    default:
                        throw new IncompatibleClassChangeError();
                }
            }
        }
        return !Minecraft.getInstance().gui.hud.isHidden() && entity != minecraft.getCameraEntity() && isVisible && !entity.isVehicle();
    }

    @NotNull
    public Identifier getTextureLocation(Player player) {
        return this.currentTexture == null ? PlayerCapability.get(player).map((cap) -> cap.getTextureLocation()).orElse(MissingTextureAtlasSprite.getLocation()) : this.currentTexture;
    }

    public void renderNameTag(Player player, Component component, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, AvatarRenderState state) {
        // 26.3 port: 名牌提交已由 vanilla EntityRenderer#submitNameDisplay 按 state 完成；
        // 计分板副标题（displayObjective 2）暂不再单独渲染。
        // TODO port: 如需保留计分板副标题，可参照 vanilla submitNameDisplay(state, ..., offset) 的 offset 重载实现。
        if (PlayerPreviewEntity.isPreviewPlayer(player)) {
            return;
        }
    }

    @Override
    public void setupRotations(Player player, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(player, poseStack, ageInTicks, rotationYaw, partialTicks);
        Entity vehicle = player.getVehicle();
        if (TouhouLittleMaidCompat.isSimplePlanesEntity(vehicle) || TouhouLittleMaidCompat.isImmersiveAircraftEntity(vehicle)) {
            poseStack.translate(0.0d, 0.5d, 0.0d);
        }
    }
}
