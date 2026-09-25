package rip.ysm.compat.simplehats;

// TODO port: stub, 原实现依赖 1.20.1 的 mod jar，恢复 compat 时替换（真实现见 common/src/main/java/rip/ysm/compat，被构建排除）

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class SimpleHatsHelper {

    private SimpleHatsHelper() {
    }

    
    public static boolean isLoaded() {
        return false;
    }

    
    public static ItemStack getHatItem(LivingEntity livingEntity) {
        return ItemStack.EMPTY;
    }
}
