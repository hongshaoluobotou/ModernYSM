package rip.ysm.compat.gun.tacz;

// TODO port: stub, 原实现依赖 1.20.1 的 mod jar，恢复 compat 时替换（真实现见 common/src/main/java/rip/ysm/compat，被构建排除）

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class TacCompat {

    private TacCompat() {
    }

    
    public static boolean isLoaded() {
        return false;
    }

    
    public static void registerControllerFunctions(CtrlBinding binding) {
        binding.livingEntityVar("tac_hold_gun", ctx -> false);
        binding.livingEntityVar("tac_gun_type", ctx -> com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool.EMPTY);
        binding.livingEntityVar("tac_gun_id", ctx -> com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool.EMPTY);
        binding.livingEntityVar("tac_is_fire", ctx -> false);
        binding.livingEntityVar("tac_is_aim", ctx -> false);
        binding.livingEntityVar("tac_is_reload", ctx -> false);
        binding.livingEntityVar("tac_is_melee", ctx -> false);
        binding.livingEntityVar("tac_is_draw", ctx -> false);
        binding.livingEntityVar("tac_fire_mode", ctx -> com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool.EMPTY);
    }

    
    public static void applyItemTransform(ItemStack stack, AnimatedGeoModel model, LivingEntity entity, PoseStack poseStack, int packedLightIn, float partialTicks) {
        {}
    }

    
    public static PlayState handleTaczAnimState(LivingEntity entity, AnimationEvent<? extends LivingAnimatable<?>> event, String animation, ILoopType loopType) {
        return null;
    }

    
    public static PlayState handleGunHoldAnimState(ItemStack stack, AnimationEvent<? extends LivingAnimatable<?>> event) {
        return null;
    }

    
    public static PlayState handleGunActionAnimState(ItemStack stack, AnimationEvent<? extends LivingAnimatable<?>> event) {
        return null;
    }

    
    public static void handleGunSound(LivingEntity entity, ItemStack stack) {
        {}
    }

    
    public static void handleItemSound(ItemStack stack) {
        {}
    }

    
    public static Identifier getGunTexture(ItemStack stack) {
        return null;
    }
}
