package com.elfmcys.yesstevemodel.util;

import com.elfmcys.yesstevemodel.client.entity.IPreviewAnimatable;
import com.elfmcys.yesstevemodel.client.bridge.RenderBridge;
import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import rip.ysm.compat.oculus.OculusCompat;

public final class CameraUtil {
    public static int getCameraType(IContext<? extends Entity> IContext) {
        if (IContext.entity() == Minecraft.getInstance().player && RenderBridge.firstPerson) {
            return IContext.mc().options.getCameraType().ordinal();
        }
        return CameraType.THIRD_PERSON_FRONT.ordinal();
    }

    /**
     * 相机实体已脱离该玩家（tweakeroo 等 freecam 换 cameraEntity 出窍）：身体从外部可见。
     * 此状态下本地玩家常被 freecam mod spoof 成 spectator/第一人称语义，但身体实际可见，
     * YSM 接管判定不应按"不可见"处理。
     */
    public static boolean isCameraDetached(Entity entity) {
        return entity == Minecraft.getInstance().player && Minecraft.getInstance().getCameraEntity() != entity;
    }

    public static boolean isFirstPerson(AnimatableEntity<? extends Entity> animatableEntity) {
        Entity entity = animatableEntity.getEntity();
        // 相机实体已不是该玩家（tweakeroo 等 freecam 换 cameraEntity 出窍）：身体从外部可见，
        // 不算第一人称——否则 ReplacePlayerRenderEvent 的 FP 门控会跳过接管，出窍后看到原版史蒂夫
        // （zergatul freecam 强制第三人称相机类型，天然不命中此分支）。
        if (isCameraDetached(entity)) {
            return false;
        }
        return entity == Minecraft.getInstance().player && RenderBridge.firstPerson && !OculusCompat.isPBRActive() && Minecraft.getInstance().options.getCameraType().ordinal() == CameraType.FIRST_PERSON.ordinal();
    }

    public static boolean isThirdPerson(IContext<? extends Entity> IContext) {
        return isThirdPersonModel(IContext.geoInstance());
    }

    public static boolean isThirdPersonModel(AnimatableEntity<?> model) {
        return (model instanceof IPreviewAnimatable) || RenderBridge.preview;
    }
}