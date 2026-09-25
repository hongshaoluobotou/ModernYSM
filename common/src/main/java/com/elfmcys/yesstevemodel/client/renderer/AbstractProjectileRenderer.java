package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.util.Color;
import com.elfmcys.yesstevemodel.geckolib3.geo.IGeoRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.EModelRenderCycle;
import com.elfmcys.yesstevemodel.geckolib3.util.IRenderCycle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

// 26.3 port: EntityRenderer<T, S extends EntityRenderState>；MultiBufferSource → GeoBufferSource。
public abstract class AbstractProjectileRenderer<TEntity extends Projectile, T extends AnimatableEntity<TEntity>> extends EntityRenderer<TEntity, EntityRenderState> implements IGeoRenderer<T> {

    public Matrix4f modelViewMatrix;

    public Matrix4f projectionMatrix;

    private IRenderCycle renderState;

    public GeoBufferSource bufferSource;

    public AbstractProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.modelViewMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();
        this.renderState = EModelRenderCycle.INITIAL;
        this.bufferSource = null;
    }

    public void render(T animatable, float entityYaw, float partialTick, PoseStack poseStack, GeoBufferSource bufferSource, int packedLight) {
        AnimationEvent<?> event = animatable.processAnimation(partialTick);
        Minecraft minecraft = Minecraft.getInstance();
        if (event != null && minecraft.player != null) {
            Projectile projectile = animatable.getEntity();
            boolean isVisible = !projectile.isInvisibleTo(minecraft.player);
            boolean zShouldEntityAppearGlowing = minecraft.shouldEntityAppearGlowing(projectile);
            RenderType renderType = getRenderType(animatable.getTextureLocation(), isVisible, zShouldEntityAppearGlowing, animatable.getCurrentModel().getGeoModel().isTranslucentTexture(0));
            if (renderType != null && (isVisible || zShouldEntityAppearGlowing)) {
                Color color = getRenderColor(animatable, partialTick, poseStack, bufferSource, null, packedLight);
                AnimatedGeoModel model = animatable.getCurrentModel();
                this.modelViewMatrix = new Matrix4f(poseStack.last().pose());
                setCurrentModelRenderCycle(EModelRenderCycle.INITIAL);
                poseStack.pushPose();
                poseStack.rotate(Axis.YP.rotationDegrees(Mth.lerp(partialTick, projectile.yRotO, projectile.getYRot()) - 90.0f));
                poseStack.rotate(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, projectile.xRotO, projectile.getXRot())));
                renderWithBoneAndRenderType(model, animatable, partialTick, renderType, poseStack, bufferSource, 0, null, packedLight, getPackedLight(projectile, 0.0f), color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
                poseStack.popPose();
            }
        }
        // 26.3 port: 原 super.render(...)（vanilla 立即渲染）已随 submit 体系移除。
    }

    @Override
    public void renderEarly(T animatable, PoseStack poseStack, float partialTick, GeoBufferSource bufferSource, VertexConsumer buffer, int packedLight, int packedOverlayIn, float red, float green, float blue, float alpha) {
        this.projectionMatrix = new Matrix4f(poseStack.last().pose());
        IGeoRenderer.super.renderEarly(animatable, poseStack, partialTick, bufferSource, buffer, packedLight, packedOverlayIn, red, green, blue, alpha);
    }

    public static int getPackedLight(Entity entity, float u) {
        return OverlayTexture.pack(OverlayTexture.u(u), OverlayTexture.v(false));
    }

    @Override
    @NotNull
    public IRenderCycle getCurrentModelRenderCycle() {
        return this.renderState;
    }

    @Override
    public void setCurrentModelRenderCycle(IRenderCycle cycle) {
        this.renderState = cycle;
    }

    @Override
    public void setCurrentRTB(GeoBufferSource bufferSource) {
        this.bufferSource = bufferSource;
    }

    @Override
    public GeoBufferSource getCurrentRTB() {
        return this.bufferSource;
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        // 本渲染器由 mixin 在 EntityRenderDispatcher.submit 处手动调用，不经由此入口。
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
