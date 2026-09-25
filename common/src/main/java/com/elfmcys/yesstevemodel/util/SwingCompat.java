package com.elfmcys.yesstevemodel.util;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SwingAnimation;

// TODO port: 26.3 挥动状态（swinging/swingTime/swingingArm 字段）移除，统一走此兼容层
public class SwingCompat {

    private SwingCompat() {
    }

    public static boolean isSwinging(LivingEntity entity) {
        return entity.isSwinging();
    }

    /**
     * 返回当前挥动手，未挥动时返回 null（等价旧 swingingArm 的"未挥动"语义）。
     */
    public static InteractionHand getSwingArm(LivingEntity entity) {
        var swing = entity.getCurrentSwing();
        return swing == null ? null : swing.hand();
    }

    /**
     * 近似旧 swingTime：挥动刚开始（进度为 0）返回 0，未挥动返回 -1。
     */
    public static int getSwingTime(LivingEntity entity) {
        var swing = entity.getCurrentSwing();
        if (swing == null) {
            return -1;
        }
        float progress = entity.getSwingAnimation(0.0f);
        return progress <= 0.0f ? 0 : Math.max(1, (int) (progress * swing.durationTicks()));
    }

    public static boolean startSwing(LivingEntity entity, InteractionHand hand) {
        return entity.swing(hand, SwingAnimation.DEFAULT, false);
    }
}
