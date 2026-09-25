package rip.ysm.compat.create;

// TODO port: stub, 原实现依赖 1.20.1 的 mod jar，恢复 compat 时替换（真实现见 common/src/main/java/rip/ysm/compat，被构建排除）

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import net.minecraft.world.entity.player.Player;

public final class CreateCompat {

    private CreateCompat() {
    }

    
    public static boolean isLoaded() {
        return false;
    }

    
    public static boolean isPlayerOnCreateContraption(Player player) {
        return false;
    }

    
    public static void registerCreateFunctions(CtrlBinding binding) {
        {}
    }
}
