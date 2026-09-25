package com.elfmcys.yesstevemodel.fabric.mixin;

import com.elfmcys.yesstevemodel.client.event.MobEffectEvent;
import net.minecraft.world.effect.MobEffectInstance;
import java.util.Collection;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.entity.Entity;

@Mixin(LivingEntity.class)
public abstract class LivingEntityEffectMixin {

    @Inject(method = "onEffectAdded", at = @At("TAIL"))
    private void ysm$onEffectAdded(MobEffectInstance instance, Entity source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        MobEffectEvent.onEffectAdded(self, instance.getEffect().value(), instance.getAmplifier());
    }

    @Inject(method = "onEffectsRemoved", at = @At("HEAD"))
    private void ysm$onEffectRemoved(Collection<MobEffectInstance> instances, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        for (MobEffectInstance instance : instances) {
            MobEffectEvent.onEffectRemoved(self, instance.getEffect().value());
        }
    }
}
