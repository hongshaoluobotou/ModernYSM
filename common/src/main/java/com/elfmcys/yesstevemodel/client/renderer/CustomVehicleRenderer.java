package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.VehicleCapability;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import rip.ysm.compat.touhoulittlemaid.TouhouLittleMaidCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.phys.Vec3;
import rip.ysm.api.entity.EntityDataBridge;

import java.util.List;

// 26.3 port: MultiBufferSource → GeoBufferSource（submit 体系）。
public class CustomVehicleRenderer {
    public static boolean renderVehicle(Entity entity, float entityYaw, float partialTick, PoseStack poseStack, GeoBufferSource bufferSource, int packedLight) {
        return VehicleCapability.get(entity).map(cap -> {
            if (cap.isModelInitialized() && cap.isModelReady()) {
                RendererManager.getVehicleRenderer().renderEntity(cap, getBodyRotation(entity, entityYaw, partialTick), partialTick, poseStack, bufferSource, packedLight);
                return false;
            }
            return true;
        }).orElse(true);
    }

    /**
     * 26.3 port: 原 ModelPreviewRenderer#renderVehicleModel（乘客在自定义载具模型上的位姿挂接）
     * 迁移到本类，供 EntityRenderDispatcherMixin 在 vanilla 渲染载具前应用乘客骨骼变换。
     */
    public static void applyPassengerPose(Entity entity, PoseStack poseStack, float partialTick) {
        Entity vehicle = entity.getVehicle();
        if (vehicle == null) {
            return;
        }
        VehicleCapability.get(vehicle).ifPresent(cap -> {
            int index;
            AnimatedGeoModel model;
            List<IBone> list;
            if (!cap.isModelInitialized() || !cap.isModelReady() || (index = vehicle.getPassengers().indexOf(entity)) < 0 || (model = cap.getCurrentModel()) == null || model.passengerGroupChains().isEmpty() || index >= model.passengerGroupChains().size() || (list = model.passengerGroupChains().get(index)) == null) {
                return;
            }
            float bodyRotation = getBodyRotation(vehicle, Mth.lerp(partialTick, vehicle.yRotO, vehicle.getYRot()), partialTick);
            poseStack.rotate(Axis.YP.rotationDegrees(180.0f - bodyRotation));
            RenderUtils.prepMatrixForLocator(poseStack, list);
            poseStack.rotate(Axis.YN.rotationDegrees(180.0f - bodyRotation));
            // 26.3 port: getPassengersRidingOffset/getMyRidingOffset 已删除，
            // 乘客竖直偏移以 getPassengerRidingPosition(entity).y（相对载具的座椅高度）近似。
            double myRidingOffset = -vehicle.getPassengerRidingPosition(entity).y;
            if (((entity instanceof Player) && PlayerCapability.get(entity).isPresent()) || TouhouLittleMaidCompat.isMaidRideable(entity)) {
                myRidingOffset -= 0.5d;
            }
            poseStack.translate(0.0d, myRidingOffset, 0.0d);
        });
    }

    public static float getBodyRotation(Entity entity, float entityYaw, float partialTick) {
        float bodyRotation = entityYaw;
        if (entity instanceof LivingEntity) {
            bodyRotation = getLivingBodyRotation((LivingEntity) entity, partialTick);
        } else if (entity instanceof AbstractMinecart minecart) {
            bodyRotation = getMinecartBodyRotation(minecart, partialTick, bodyRotation);
        }
        return bodyRotation;
    }

    /**
     * 矿车车身朝向（1.20.1 {@code getMinecartBodyRotation} 的 26.3 等价实现）。
     *
     * <p>1.20.1 直接调 {@code AbstractMinecart#getPos/getPosOffs}；26.3 把这些搬进了
     * {@code MinecartBehavior} 子类，且新增了 "New" 物理（沿轨道吸附）。这里逐值对照
     * vanilla {@code AbstractMinecartRenderer#newExtractState/oldExtractState}：</p>
     * <ul>
     *   <li>{@code NewMinecartBehavior}：有插值步进时用 {@code getCartLerpYRot}，否则实体 yaw；</li>
     *   <li>{@code OldMinecartBehavior}：取插值位置 → 前后各偏移 0.3 的两个轨道点 →
     *       {@code atan2(dz, dx)}（与 1.20.1 完全同一算式）。</li>
     * </ul>
     */
    private static float getMinecartBodyRotation(AbstractMinecart minecart, float partialTick, float defaultYaw) {
        if (minecart.getBehavior() instanceof NewMinecartBehavior newBehavior) {
            return newBehavior.cartHasPosRotLerp() ? newBehavior.getCartLerpYRot(partialTick) : defaultYaw;
        }
        if (minecart.getBehavior() instanceof OldMinecartBehavior oldBehavior) {
            double interpX = Mth.lerp(partialTick, minecart.xo, minecart.getX());
            double interpY = Mth.lerp(partialTick, minecart.yo, minecart.getY());
            double interpZ = Mth.lerp(partialTick, minecart.zo, minecart.getZ());
            Vec3 pos = oldBehavior.getPos(interpX, interpY, interpZ);
            if (pos == null) {
                return defaultYaw;
            }
            Vec3 front = oldBehavior.getPosOffs(interpX, interpY, interpZ, 0.3);
            Vec3 back = oldBehavior.getPosOffs(interpX, interpY, interpZ, -0.3);
            if (front == null) {
                front = pos;
            }
            if (back == null) {
                back = pos;
            }
            Vec3 dir = back.add(-front.x, -front.y, -front.z);
            if (dir.length() != 0.0) {
                return (float) (Math.atan2(dir.z, dir.x) * 180.0 / Math.PI);
            }
        }
        return defaultYaw;
    }

    private static float getLivingBodyRotation(LivingEntity entity, float partialTick) {
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);

        if (entity.isPassenger() && entity.getVehicle() != null && EntityDataBridge.shouldRiderSit(entity.getVehicle())) {
            Entity vehicle = entity.getVehicle();
            if (vehicle instanceof LivingEntity livingVehicle) {
                float vehicleBodyYaw = Mth.rotLerp(partialTick, livingVehicle.yBodyRotO, livingVehicle.yBodyRot);
                float yawDiff = Mth.clamp(Mth.wrapDegrees(headYaw - vehicleBodyYaw), -85.0f, 85.0f);
                bodyYaw = headYaw - yawDiff;

                if (yawDiff * yawDiff > 2500.0f) {
                    bodyYaw += yawDiff * 0.2f;
                }
            }
        }
        return bodyYaw;
    }

    /* 26.3 port: 矿车车身朝向已按 vanilla AbstractMinecartRenderer 的 New/Old 两套行为复刻，
       见上方 getMinecartBodyRotation。 */
}
