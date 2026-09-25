package rip.ysm.compat.carryon;

// TODO port: stub, 原实现依赖 1.20.1 的 mod jar，恢复 compat 时替换（真实现见 common/src/main/java/rip/ysm/compat，被构建排除）

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class CarryOnDataHelper {

    public enum CarryType {
        ENTITY,
        BLOCK,
        PLAYER,
        NONE
    }

    private CarryOnDataHelper() {
    }

    
    public static boolean isPlayerCarrying(LivingEntity livingEntity) {
        return false;
    }

    
    public static CarryType getCarryType(Player player) {
        return CarryType.NONE;
    }
}
