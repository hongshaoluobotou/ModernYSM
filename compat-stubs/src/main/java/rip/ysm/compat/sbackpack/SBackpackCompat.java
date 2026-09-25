package rip.ysm.compat.sbackpack;

// TODO port: stub, 原实现依赖 1.20.1 的 mod jar，恢复 compat 时替换（真实现见 common/src/main/java/rip/ysm/compat，被构建排除）

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;

public final class SBackpackCompat {

    private SBackpackCompat() {
    }

    
    public static boolean isLoaded() {
        return false;
    }

    
    public static void setupRenderLayers() {
        {}
    }

    
    public static Optional<Pair<String, String>> getInCompatibleInfo() {
        return Optional.empty();
    }

    
    public static void registerControllerFunctions(CtrlBinding binding) {
        {}
    }

    
    public static ItemStack getBackpackItem(LivingEntity livingEntity) {
        return ItemStack.EMPTY;
    }
}
