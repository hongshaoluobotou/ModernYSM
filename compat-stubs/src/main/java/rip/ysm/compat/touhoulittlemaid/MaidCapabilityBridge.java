package rip.ysm.compat.touhoulittlemaid;

// TODO port: stub, 原实现依赖 1.20.1 的 mod jar，恢复 compat 时替换（真实现见 common/src/main/java/rip/ysm/compat，被构建排除）

import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class MaidCapabilityBridge {

    private MaidCapabilityBridge() {
    }

    
    public static Optional<Object> get(Entity entity) {
        return Optional.empty();
    }
}
