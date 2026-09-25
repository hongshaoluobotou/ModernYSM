package com.elfmcys.yesstevemodel.capability.fabric;

import net.minecraft.nbt.CompoundTag;

/**
 * 自研附加数据（替代 Cardinal Components 的 {@code Component} 接口）。
 * 所有附加组件实现该接口，由 {@link YsmAttachments} 负责挂载、持久化与复制。
 */
public interface YsmComponent {

    void readFromNbt(CompoundTag tag);

    void writeToNbt(CompoundTag tag);
}
