package rip.ysm.compat.slashblade;

// TODO port: stub, 原实现依赖 1.20.1 的 mod jar，恢复 compat 时替换（真实现见 common/src/main/java/rip/ysm/compat，被构建排除）

import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class SlashBladeRenderer {

    private SlashBladeRenderer() {
    }

    
    public static void renderOnEntity(LivingEntity entity, AnimatedGeoModel model, PoseStack poseStack, Object bufferSource, int packedLight, ItemStack stack, float partialTick) {
        {}
    }

    
    public static void renderRightWaist(AnimatedGeoModel model, PoseStack poseStack, Object bufferSource, int packedLight, ItemStack stack) {
        {}
    }
}
