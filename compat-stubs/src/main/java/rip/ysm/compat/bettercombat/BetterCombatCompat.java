package rip.ysm.compat.bettercombat;

// TODO port: stub, 原实现依赖 1.20.1 的 mod jar，恢复 compat 时替换（真实现见 common/src/main/java/rip/ysm/compat，被构建排除）

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;

public final class BetterCombatCompat {

    private BetterCombatCompat() {
    }

    
    public static boolean isLoaded() {
        return false;
    }

    
    public static void registerBindings(CtrlBinding binding) {
        binding.clientPlayerEntityVar("bcombat_attack_animation", ctx -> com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool.EMPTY);
    }
}
