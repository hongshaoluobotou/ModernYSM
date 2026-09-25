package rip.ysm.compat.slashblade;

// TODO port: stub, 原实现依赖 1.20.1 的 mod jar，恢复 compat 时替换（真实现见 common/src/main/java/rip/ysm/compat，被构建排除）

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class SlashBladeCompat {

    private SlashBladeCompat() {
    }

    
    public static boolean isLoaded() {
        return false;
    }

    
    public static boolean isSlashBladeItem(ItemStack itemStack) {
        return false;
    }

    
    public static String getComboAnimName(AnimationEvent<? extends LivingAnimatable<?>> event) {
        return null;
    }

    
    public static PlayState handleSlashBladeAnim(LivingEntity livingEntity, AnimationEvent<? extends LivingAnimatable<?>> event, String str, ILoopType loopType) {
        return null;
    }

    
    public static void registerControllerFunctions(CtrlBinding ctrlBinding) {
        {}
    }

    
    public static boolean hasNewApi() {
        return false;
    }
}
