package rip.ysm.compat.immersivemelodies;

// TODO port: stub, 原实现依赖 1.20.1 的 mod jar，恢复 compat 时替换（真实现见 common/src/main/java/rip/ysm/compat，被构建排除）

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import net.minecraft.world.entity.LivingEntity;

public final class ImmersiveMelodiesCompat {

    public static final class ImmersiveMelodiesData {
        public float pitch = 0f;
        public float volume = 0f;
        public float current = 0f;
        public long delta = 0L;
        public long time = 0L;
    }

    private ImmersiveMelodiesCompat() {
    }

    
    public static boolean isLoaded() {
        return false;
    }

    
    public static void updateMelodyProgress(LivingEntity livingEntity, ImmersiveMelodiesData imData) {
        {}
    }

    
    public static void registerBindings(CtrlBinding binding) {
        binding.livingEntityVar("im_pitch", ctx -> 0.0f);
        binding.livingEntityVar("im_volume", ctx -> 0.0f);
        binding.livingEntityVar("im_current", ctx -> 0.0f);
        binding.livingEntityVar("im_delta", ctx -> 0L);
        binding.livingEntityVar("im_time", ctx -> 0L);
    }
}
