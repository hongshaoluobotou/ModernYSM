package rip.ysm.compat.cosmeticarmorreworked;

// 原 1.20.1 实现依赖 cosmetic-armor-reworked mod 提供饰品盔甲槽兜底；无该 mod 时语义即
// entity.getItemBySlot(slot)。26.3 移植期此处曾被替换为恒返回 EMPTY 的空存根，导致
// has_helmet/has_chest_plate 等 molang 查询与 ArmorPredicate 全部恒 false——模型包的
// 盔甲动画（如戴头盔触发 head 动画）完全失效（真机报"戴上钻石头盔无动画/模型变化"）。
// 现恢复 1.20.1 fabric 实现的兜底语义；若将来恢复 cosmetic-armor-reworked compat，
// 在此之前追加饰品槽查询即可。

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
// 26.3 已删除 ElytraItem 类，鞘翅判定改为 DataComponents.GLIDER 组件（vanilla 鞘翅物品携带该组件）。

public final class CosmeticArmorHelper {

    private CosmeticArmorHelper() {
    }

    public static ItemStack getArmorItem(LivingEntity entity, EquipmentSlot slot) {
        return entity.getItemBySlot(slot);
    }

    public static ItemStack getElytraItem(LivingEntity livingEntity) {
        ItemStack chest = livingEntity.getItemBySlot(EquipmentSlot.CHEST);
        return chest.has(DataComponents.GLIDER) ? chest : ItemStack.EMPTY;
    }
}
