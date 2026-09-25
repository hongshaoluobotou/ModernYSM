package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import rip.ysm.compat.touhoulittlemaid.TouhouLittleMaidCompat;
import rip.ysm.compat.gun.swarfare.SWarfareCompat;
import com.elfmcys.yesstevemodel.client.entity.PlayerPreviewEntity;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
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
        renderEntityWithTexture(capability, renderEvent.getTextureLocation(), player.getYRot(), partialTick, poseStack, bufferSource, packedLight);
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
