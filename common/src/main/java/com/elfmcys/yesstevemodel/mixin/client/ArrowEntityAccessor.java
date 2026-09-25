package com.elfmcys.yesstevemodel.mixin.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.ArrayList;
import java.util.List;

/**
 * 26.3：Arrow 不再持有 {@code effects} 字段（药水效果改为从拾取物 ItemStack 的 PotionContents 组件派生）。
 * 由 @Accessor 改为普通辅助类，效果从 pickupItemStack 读取；无药水时返回空列表（保守降级）。
 */
public final class ArrowEntityAccessor {
    private ArrowEntityAccessor() {
    }

    public static List<MobEffectInstance> getEffects(Arrow arrow) {
        ItemStack item = arrow.getPickupItemStackOrigin();
        if (item.isEmpty() || item.getItem() != Items.TIPPED_ARROW) {
            return List.of();
        }
        List<MobEffectInstance> effects = new ArrayList<>();
        PotionContents contents = item.get(DataComponents.POTION_CONTENTS);
        if (contents != null) {
            contents.getAllEffects().forEach(effects::add);
        }
        return effects;
    }
}
