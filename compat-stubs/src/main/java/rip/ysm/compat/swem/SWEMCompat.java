package rip.ysm.compat.swem;

// TODO port: stub, 原实现依赖 1.20.1 的 mod jar，恢复 compat 时替换（真实现见 common/src/main/java/rip/ysm/compat，被构建排除）

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import net.minecraft.world.entity.LivingEntity;

public final class SWEMCompat {

    private SWEMCompat() {
    }

    
    public static boolean isLoaded() {
        return false;
    }

    
    public static String getHorseGaitName(LivingEntity livingEntity) {
        return null;
    }

    
    public static void registerControllerFunctions(CtrlBinding ctrlBinding) {
        {}
    }
}
