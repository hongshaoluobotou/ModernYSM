package rip.ysm.compat.touhoulittlemaid;

// TODO port: stub, 原实现依赖 1.20.1 的 mod jar，恢复 compat 时替换（真实现见 common/src/main/java/rip/ysm/compat，被构建排除）

import com.elfmcys.yesstevemodel.network.message.FeedbackData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

public final class TouhouMaidCompat {

    private TouhouMaidCompat() {
    }

    
    public static boolean isLoaded() {
        return false;
    }

    
    public static void init() {
        {}
    }

    
    public static boolean isMaidEntity(Entity entity) {
        return false;
    }

    
    public static void handleProjectileOwner(Projectile projectile, Entity entity) {
        {}
    }

    
    public static void registerAnimationRoulette(Entity entity, String str, int i) {
        {}
    }

    
    public static void applyFeedback(Entity entity, FeedbackData message) {
        {}
    }

       @Environment(EnvType.CLIENT)
    public static void playMaidAnimation(Entity entity, String str) {
        {}
    }
}
