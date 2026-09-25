package rip.ysm.compat.parcool;

// TODO port: stub, 原实现依赖 1.20.1 的 mod jar，恢复 compat 时替换（真实现见 common/src/main/java/rip/ysm/compat，被构建排除）

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.IAnimationController;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import java.util.function.BiFunction;

public final class ParcoolCompat {

    private ParcoolCompat() {
    }

    
    public static boolean isLoaded() {
        return false;
    }

    
    public static Optional<Pair<String, String>> getInCompatibleInfo() {
        return Optional.empty();
    }

    
    public static Optional<BiFunction<String, CustomPlayerEntity, IAnimationController<CustomPlayerEntity>>> getControllerFactory() {
        return Optional.empty();
    }

    
    public static boolean isPlayerParcooling(Player player) {
        return false;
    }

    
    public static String getActionName(Player player) {
        return null;
    }

    
    public static void registerBindings(CtrlBinding binding) {
        {}
    }
}
