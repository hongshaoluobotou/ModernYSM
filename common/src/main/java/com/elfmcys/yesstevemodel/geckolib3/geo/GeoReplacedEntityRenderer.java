package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.client.renderer.GeoBufferSource;
import com.elfmcys.yesstevemodel.capability.VehicleCapability;
import com.elfmcys.yesstevemodel.client.bridge.RenderBridge;
import com.elfmcys.yesstevemodel.util.CameraUtil;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.util.Color;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import com.elfmcys.yesstevemodel.geckolib3.util.EModelRenderCycle;
import com.elfmcys.yesstevemodel.geckolib3.util.IRenderCycle;
import com.elfmcys.yesstevemodel.mixin.client.LivingEntityAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import rip.ysm.api.client.RenderLivingBridge;

import java.util.List;
import java.util.Optional;

// 26.3 port: LivingEntityRenderer 泛型改为 (TEntity, AvatarRenderState, PlayerModel)，
// 渲染入口从 render(...) 改为 submit(...)；MultiBufferSource → GeoBufferSource（submitCustomGeometry 延迟提交）。
public abstract class GeoReplacedEntityRenderer<TEntity extends LivingEntity, T extends LivingAnimatable<TEntity>> extends LivingEntityRenderer<TEntity, AvatarRenderState, PlayerModel> implements IGeoRenderer<T> {

    public final List<GeoLayerRenderer<T>> layerRenderers = new ObjectArrayList<>();

    public Matrix4f dispatchedMat = new Matrix4f();

    public Matrix4f renderEarlyMat = new Matrix4f();

    public GeoBufferSource rtb;

    private IRenderCycle currentModelRenderCycle = EModelRenderCycle.INITIAL;

    /**
     * 26.3 port: submit 体系下层渲染（头盔/鞘翅/鹦鹉）需要 AvatarRenderState；
     * 由 {@link #extractLayerRenderState} 在每次实体渲染前从真实实体提取并暂存。
     */
    @Nullable
    private AvatarRenderState layerRenderState;

    @Nullable
    private SubmitNodeCollector currentCollector;

    public GeoReplacedEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5f);
        this.rtb = null;
    }

    public static int packOverlayCoords(LivingEntity entity, float u) {
        return OverlayTexture.pack(OverlayTexture.u(u), OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0));
    }

    @Override
    @NotNull
    public IRenderCycle getCurrentModelRenderCycle() {
        return this.currentModelRenderCycle;
    }

    @Override
    public void setCurrentModelRenderCycle(IRenderCycle cycle) {
        this.currentModelRenderCycle = cycle;
    }

    @Override
    public void renderEarly(T animatable, PoseStack poseStack, float partialTick, GeoBufferSource bufferSource, VertexConsumer buffer, int packedLight, int packedOverlayIn, float red, float green, float blue, float alpha) {
        // 使用 .set 来避免每次渲染创建新的 Matrix4f, 减少 allocation rate
        this.renderEarlyMat.set(poseStack.last().pose());
        IGeoRenderer.super.renderEarly(animatable, poseStack, partialTick, bufferSource, buffer, packedLight, packedOverlayIn, red, green, blue, alpha);
    }

    public void renderEntity(T t, float entityYaw, float partialTick, PoseStack poseStack, GeoBufferSource bufferSource, int packedLight) {
        renderEntityWithTexture(t, null, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    public void renderEntityWithTexture(T t, @Nullable Identifier resourceLocation, float entityYaw, float partialTick, PoseStack poseStack, GeoBufferSource multiBufferSource, int packedLight) {
        Direction bedOrientation;
        boolean fireRenderEvents = !RenderBridge.preview;
        if (fireRenderEvents && RenderLivingBridge.firePre(t.getEntity(), this, partialTick, poseStack, multiBufferSource, packedLight)) {
            return;
        }
        AnimationEvent<?> event = t.processAnimation(partialTick);
        TEntity entity = t.getEntity();
        Minecraft minecraft = Minecraft.getInstance();
        if (event != null && minecraft.player != null) {
            EntityModelData modelData = event.getModelData();
            // 使用 .set 来避免每次渲染创建新的 Matrix4f, 减少 allocation rate
            this.dispatchedMat.set(poseStack.last().pose());
            setCurrentModelRenderCycle(EModelRenderCycle.INITIAL);
            poseStack.pushPose();
            if (entity.getPose() == Pose.SLEEPING && (bedOrientation = entity.getBedOrientation()) != null) {
                float eyeHeight = entity.getEyeHeight(Pose.STANDING) - 0.1f;
                poseStack.translate((-bedOrientation.getStepX()) * eyeHeight, 0.0f, (-bedOrientation.getStepZ()) * eyeHeight);
            }
            setupRotations(entity, poseStack, modelData.lerpedAge, modelData.lerpBodyRot, partialTick);
            if (t.getEntity().getVehicle() != null) {
                VehicleCapability.get(t.getEntity().getVehicle()).ifPresent(cap -> {
                    Vector3f vector3f = cap.getExpressionOffset();
                    if (vector3f != null) {
                        poseStack.rotate(new Quaternionf().rotateZYX(vector3f.z, 0.0f, vector3f.x).invert());
                    }
                });
            }
            preRenderCallback(entity, poseStack, partialTick);
            poseStack.translate(0.0f, 0.01f, 0.0f);
            AnimatedGeoModel animatedGeoModel = t.getCurrentModel();
            int textureIndex = resourceLocation == null ? t.getTextureIndex() : 0;
            RenderType renderType = getRenderType(resourceLocation == null ? t.getTextureLocation() : resourceLocation, !entity.isInvisibleTo(minecraft.player), minecraft.shouldEntityAppearGlowing(entity), t.getCurrentModel().getGeoModel().isTranslucentTexture(textureIndex));
            boolean useExtraPlayer = t.isRenderLayersFirst();
            Color color = getRenderColor(t, partialTick, poseStack, multiBufferSource, null, packedLight);
            renderWithBone(animatedGeoModel, t, partialTick, poseStack, multiBufferSource, null, packedLight, packOverlayCoords(entity, getHurtOverlayProgress(entity, partialTick)), color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
            if (useExtraPlayer && (!entity.isSpectator() || CameraUtil.isCameraDetached(entity))) {
                renderLayers(t, partialTick, poseStack, multiBufferSource, packedLight, event, modelData);
            }
            if (renderType != null) {
                renderWithBoneAndRenderType(animatedGeoModel, t, partialTick, renderType, poseStack, multiBufferSource, textureIndex, null, packedLight, packOverlayCoords(entity, getHurtOverlayProgress(entity, partialTick)), color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
            }
            if (!useExtraPlayer && (!entity.isSpectator() || CameraUtil.isCameraDetached(entity))) {
                renderLayers(t, partialTick, poseStack, multiBufferSource, packedLight, event, modelData);
            }
            poseStack.popPose();
        }
        if (fireRenderEvents) {
            RenderLivingBridge.firePost(entity, this, partialTick, poseStack, multiBufferSource, packedLight);
        }
    }

    public void renderLayers(T entity, float partialTick, PoseStack poseStack, GeoBufferSource bufferSource, int packedLightIn, AnimationEvent<?> event, EntityModelData data) {
        for (GeoLayerRenderer<T> layerRenderer : this.layerRenderers) {
            layerRenderer.render(poseStack, this.currentCollector, bufferSource, packedLightIn, entity, event.getLimbSwing(), event.getLimbSwingAmount(), partialTick, data.lerpedAge, data.rawNetHeadYaw, data.rawHeadPitch, getLayerRenderState());
        }
    }

    @Nullable
    public SubmitNodeCollector getCurrentCollector() {
        return this.currentCollector;
    }

    public void setCurrentCollector(@Nullable SubmitNodeCollector collector) {
        this.currentCollector = collector;
    }

    /**
     * 每次渲染前从真实实体提取一份 AvatarRenderState 供层渲染使用
     * （等价 vanilla {@code extractRenderState}，同时暂存以便 {@link #setupRotations} 等复用）。
     */
    public AvatarRenderState extractLayerRenderState(TEntity entity, float partialTick) {
        AvatarRenderState state = new AvatarRenderState();
        this.extractRenderState(entity, state, partialTick);
        this.layerRenderState = state;
        return state;
    }

    @Nullable
    public AvatarRenderState getLayerRenderState() {
        return this.layerRenderState;
    }

    public float getHurtOverlayProgress(TEntity entity, float partialTick) {
        return 0.0f;
    }

    public void preRenderCallback(TEntity entity, PoseStack poseStack, float partialTick) {
    }

    public void setupRotations(TEntity tentity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
        int t = tentity.deathTime;
        boolean zIsAutoSpinAttack = tentity.isAutoSpinAttack();
        if (t > 0) {
            tentity.deathTime = 0;
        }
        if (zIsAutoSpinAttack) {
            ((LivingEntityAccessor) tentity).invokeSetLivingEntityFlag(4, false);
        }
        if (tentity.onClimbable()) {
            Optional<BlockPos> lastClimbablePos = tentity.getLastClimbablePos();
            if (lastClimbablePos.isPresent()) {
                Optional<Direction> optionalValue = tentity.level().getBlockState(lastClimbablePos.get()).getOptionalValue(HorizontalDirectionalBlock.FACING);
                if (optionalValue.isPresent()) {
                    rotationYaw = optionalValue.get().getOpposite().get2DDataValue() * 90;
                }
            }
        }
        // 26.3 port: 原 super.setupRotations(entity, ...) 已改为状态签名 (S, PoseStack, bodyRot, scale)，
        // 这里按 26.3 vanilla LivingEntityRenderer#setupRotations 的逻辑以内联方式实现（输入为真实实体）。
        float bodyRot = 180.0f - rotationYaw;
        if (tentity.isFullyFrozen()) {
            bodyRot += (float) (Math.cos(Mth.floor(ageInTicks) * 3.25f) * Math.PI * 0.4f);
        }
        if (tentity.getPose() != Pose.SLEEPING) {
            poseStack.rotateDegrees(Axis.YP, bodyRot);
        }
        if (t > 0) {
            float fall = (t + partialTicks - 1.0f) / 20.0f * 1.6f;
            fall = Mth.sqrt(fall);
            if (fall > 1.0f) {
                fall = 1.0f;
            }
            poseStack.rotateDegrees(Axis.ZP, fall * 90.0f);
        } else if (zIsAutoSpinAttack) {
            poseStack.rotateDegrees(Axis.XP, -90.0f - tentity.getXRot(partialTicks));
            poseStack.rotateDegrees(Axis.YP, ageInTicks * -75.0f);
        } else if (tentity.getPose() == Pose.SLEEPING) {
            Direction bedOrientation = tentity.getBedOrientation();
            poseStack.rotateDegrees(Axis.YP, bedOrientation != null ? sleepDirectionToRotation(bedOrientation) : rotationYaw);
            poseStack.rotateDegrees(Axis.ZP, 90.0f);
        }
        if (t > 0) {
            tentity.deathTime = t;
        }
        if (zIsAutoSpinAttack) {
            ((LivingEntityAccessor) tentity).invokeSetLivingEntityFlag(4, true);
        }
    }

    private static float sleepDirectionToRotation(Direction direction) {
        return switch (direction) {
            case SOUTH -> 90.0f;
            case WEST -> 0.0f;
            case NORTH -> 270.0f;
            case EAST -> 180.0f;
            default -> 0.0f;
        };
    }

    public boolean shouldShowName(TEntity entity) {
        // 26.3 port: 原 Minecraft.renderNames() 已删除，等价判断为 !hud.isHidden()（参照 vanilla shouldShowName）。
        double d = entity.isDiscrete() ? 32.0d : 64.0d;
        return this.entityRenderDispatcher.distanceToSqr(entity) < d * d && entity == this.entityRenderDispatcher.crosshairPickEntity && entity.hasCustomName() && !Minecraft.getInstance().gui.hud.isHidden();
    }

    public final boolean addLayerRenderer(GeoLayerRenderer<T> layerRenderer) {
        return this.layerRenderers.add(layerRenderer);
    }

    @Override
    public GeoBufferSource getCurrentRTB() {
        return this.rtb;
    }

    @Override
    public void setCurrentRTB(GeoBufferSource bufferSource) {
        this.rtb = bufferSource;
    }

    // ==== 26.3 EntityRenderer 抽象方法实现（本渲染器当前通过 mixin 手动调用，不走 vanilla 注册表）====

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return MissingTextureAtlasSprite.getLocation();
    }

    @Override
    public void submit(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        // 本渲染器由 mixin 在 EntityRenderDispatcher.submit 处手动调用（见 mixin/client/EntityRenderDispatcherMixin），
        // 不经由此入口；保留实现以满足 vanilla 抽象方法。
        // TODO port: 26.3 渲染注册链后续可改为注册正式 EntityRenderer 走 vanilla dispatch。
        setCurrentRenderState(state);
        setCurrentCollector(submitNodeCollector);
        GeoBufferSource bufferSource = new GeoBufferSource();
        setCurrentRTB(bufferSource);
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private void setCurrentRenderState(AvatarRenderState state) {
        this.layerRenderState = state;
    }
}
