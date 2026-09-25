package rip.ysm.compat.touhoulittlemaid;

// TODO port: stub, 原实现依赖 1.20.1 的 mod jar，恢复 compat 时替换（真实现见 common/src/main/java/rip/ysm/compat，被构建排除）

import com.elfmcys.yesstevemodel.client.animation.molang.TLMBinding;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.client.model.ModelResourceBundle;
import com.elfmcys.yesstevemodel.client.model.PlayerModelBundle;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public final class TouhouLittleMaidCompat {

    private TouhouLittleMaidCompat() {
    }

    
    public static boolean isLoaded() {
        return false;
    }

    
    public static boolean isMaidEntity(Entity entity) {
        return false;
    }

    
    public static boolean isMaidRideable(Entity entity) {
        return false;
    }

    
    public static boolean isSimplePlanesEntity(Entity entity) {
        return false;
    }

    
    public static boolean isImmersiveAircraftEntity(Entity entity) {
        return false;
    }

    
    public static boolean isMaidItem(Item item) {
        return false;
    }

    
    public static String getMaidEntityId(Entity entity) {
        return null;
    }

    
    public static boolean isMaidSitting(LivingEntity livingEntity) {
        return false;
    }

    
    public static void registerMaidAnimStates(TLMBinding tlmBinding) {
        {}
    }

    
    public static PlayState handleMaidInteraction(AnimationEvent<LivingAnimatable<?>> event, LivingEntity livingEntity, Entity entity) {
        return null;
    }

    
    public static boolean isMaidChatAvailable() {
        return false;
    }

    
    public static void openMaidChat() {
        {}
    }

    
    public static Object buildControllers(PlayerModelBundle modelBundle, ModelResourceBundle resourceBundle) {
        return null;
    }

    
    @Nullable
    public static Object getMaidPreviewRenderer(LivingAnimatable<?> animatable) { // TODO port 26.3: 返回类型原为 GeoReplacedEntityRenderer（geckolib3.geo 根目录，渲染层恢复后改回）
        return null;
    }
}
