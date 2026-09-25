package com.elfmcys.yesstevemodel.mixin;

import com.elfmcys.yesstevemodel.capability.fabric.YsmAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 自研附加数据持久化（替代 Cardinal Components 的 NBT 读写钩子）。
 * 26.3 中 {@code Entity#saveWithoutId/load} 已改用 ValueOutput/ValueInput，
 * 通过 {@link TagValueOutputAccessor}/{@link TagValueInputAccessor} 拿到底层 CompoundTag 再读写。
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "saveWithoutId(Lnet/minecraft/world/level/storage/ValueOutput;)V", at = @At("RETURN"))
    private void ysm$saveWithoutId(ValueOutput output, CallbackInfo ci) {
        if (output instanceof TagValueOutput) {
            CompoundTag root = new CompoundTag();
            YsmAttachments.writeNbt((Entity) (Object) this, root);
            if (!root.isEmpty()) {
                ((TagValueOutputAccessor) (Object) output).getOutput().put("yes_steve_model", root);
            }
        }
    }

    @Inject(method = "load(Lnet/minecraft/world/level/storage/ValueInput;)V", at = @At("RETURN"))
    private void ysm$load(ValueInput input, CallbackInfo ci) {
        if (input instanceof TagValueInput) {
            CompoundTag raw = ((TagValueInputAccessor) (Object) input).getInput();
            CompoundTag root = raw.getCompoundOrEmpty("yes_steve_model");
            // legacyRoot：旧 Cardinal Components 把组件平铺在实体 NBT 下（键如 yes_steve_model:star_models）
            YsmAttachments.readNbt((Entity) (Object) this, root, raw);
        }
    }
}
