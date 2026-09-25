package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.ProjectileCapability;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.projectile.Projectile;

// 26.3 port: MultiBufferSource → GeoBufferSource（submit 体系）。
public class CustomProjectileRenderer {
    public static boolean renderProjectile(Projectile projectile, float entityYaw, float partialTick, PoseStack poseStack, GeoBufferSource bufferSource, int packedLight) {
        return ProjectileCapability.get(projectile).map(cap -> {
            if (cap.isModelInitialized() && cap.isModelReady()) {
                RendererManager.getProjectileRenderer().render(cap, entityYaw, partialTick, poseStack, bufferSource, packedLight);
                return false;
            }
            return true;
        }).orElse(true);
    }
}
