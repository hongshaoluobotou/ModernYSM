package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.client.renderer.GeoBufferSource;
import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.util.Color;
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
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

// 26.3 port: EntityRenderer<T, S extends EntityRenderState>；MultiBufferSource → GeoBufferSource。
public abstract class GeoEntityRenderer<TEntity extends Entity, T extends AnimatableEntity<TEntity>> extends EntityRenderer<TEntity, EntityRenderState> implements IGeoRenderer<T> {

    public Matrix4f worldMatrix;

    public Matrix4f modelMatrix;

    private IRenderCycle renderState;

    public GeoBufferSource bufferSource;

    public GeoEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.worldMatrix = new Matrix4f();
        this.modelMatrix = new Matrix4f();
        this.renderState = EModelRenderCycle.INITIAL;
        this.bufferSource = null;
    }

    public void renderEntity(T t, float f, float f2, PoseStack poseStack, GeoBufferSource multiBufferSource, int i) {
        AnimationEvent<?> event = t.processAnimation(f2);
        Minecraft minecraft = Minecraft.getInstance();
        if (event != null && minecraft.player != null) {
            Entity entity = t.getEntity();
            boolean z = !entity.isInvisibleTo(minecraft.player);
            boolean zShouldEntityAppearGlowing = minecraft.shouldEntityAppearGlowing(entity);
            RenderType renderType = getRenderType(t.getTextureLocation(), z, zShouldEntityAppearGlowing, t.getCurrentModel().getGeoModel().isTranslucentTexture(0));
            if (renderType != null && (z || zShouldEntityAppearGlowing)) {
                Color color = getRenderColor(t, f2, poseStack, multiBufferSource, null, i);
                AnimatedGeoModel model = t.getCurrentModel();
                this.worldMatrix = new Matrix4f(poseStack.last().pose());
                setCurrentModelRenderCycle(EModelRenderCycle.INITIAL);
                poseStack.pushPose();
                poseStack.rotate(Axis.YP.rotationDegrees(180.0f - f));
                renderWithBoneAndRenderType(model, t, f2, renderType, poseStack, multiBufferSource, 0, null, i, packOverlayCoords(entity, 0.0f), color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
                poseStack.popPose();
            }
        }
        // 26.3 port: 原 super.render(...)（vanilla 立即渲染 + 名牌）已随 submit 体系移除；
        // 名牌/火焰/阴影由 EntityRenderDispatcherMixin 的替换路径按 vanilla submit 逻辑补交。
    }

    @Override
    public void renderEarly(T animatable, PoseStack poseStack, float partialTick, GeoBufferSource bufferSource, VertexConsumer buffer, int packedLight, int packedOverlayIn, float red, float green, float blue, float alpha) {
        this.modelMatrix = new Matrix4f(poseStack.last().pose());
        IGeoRenderer.super.renderEarly(animatable, poseStack, partialTick, bufferSource, buffer, packedLight, packedOverlayIn, red, green, blue, alpha);
    }

    public static int packOverlayCoords(Entity entity, float f) {
        return OverlayTexture.pack(OverlayTexture.u(f), OverlayTexture.v(false));
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
