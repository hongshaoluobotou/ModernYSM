package com.elfmcys.yesstevemodel.client.animation.molang.functions.ysm;

import rip.ysm.compat.cosmeticarmorreworked.CosmeticArmorHelper;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.funciton.entity.LivingEntityFunction;
import com.elfmcys.yesstevemodel.geckolib3.util.MolangUtils;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public class DumpEquippedItem extends LivingEntityFunction {
    @Override
    public Object eval(ExecutionContext<IContext<LivingEntity>> context, ArgumentCollection arguments) {
        EquipmentSlot slot;
        Identifier key;
        if (!context.entity().isDebugMode() || (slot = MolangUtils.parseSlotType(context, arguments, 0)) == null) {
            return null;
        }
        ItemStack stack = CosmeticArmorHelper.getArmorItem(context.entity().entity(), slot);
        if (stack.isEmpty() || (key = BuiltInRegistries.ITEM.getKey(stack.getItem())) == null) {
            return null;
        }
        context.entity().logWarningComponent(Component.literal("Display ").append(ComponentUtils.copyOnClickText(stack.getItem().getName(stack).getString(99))));
        context.entity().logWarningComponent(Component.literal("Name ").append(ComponentUtils.copyOnClickText(key.toString())));
        BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem()).tags().forEach(tagKey -> {
            context.entity().logWarningComponent(Component.literal("Tag ").append(ComponentUtils.copyOnClickText(tagKey.location().toString())));
        });
        // TODO port: 26.3 附魔不再以 NBT 存储，改用 ItemEnchantments 组件
        for (var entry : stack.getEnchantments().entrySet()) {
            Holder<Enchantment> enchantmentHolder = entry.getKey();
            context.entity().logWarningComponent(Component.literal("Enchantment: display ").append(ComponentUtils.copyOnClickText(Enchantment.getFullname(enchantmentHolder, entry.getIntValue()).getString(99))).append(Component.literal("  name ").append(ComponentUtils.copyOnClickText(enchantmentHolder.unwrapKey().map(k -> k.identifier().toString()).orElse("")))));
        }
        return null;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}
