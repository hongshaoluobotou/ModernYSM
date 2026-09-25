package com.elfmcys.yesstevemodel.mixin;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 26.3：AbstractArrow#inGround 字段改为 private，访问走 isInGround() 方法。
 */
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
    @Invoker("isInGround")
    boolean ysm$isInGround();
}
