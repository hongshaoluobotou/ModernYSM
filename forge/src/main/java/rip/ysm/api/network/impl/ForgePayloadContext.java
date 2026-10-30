package rip.ysm.api.network.impl;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import org.jetbrains.annotations.Nullable;
import rip.ysm.api.network.PacketContext;

/**
 * Forge 侧的 {@link PacketContext} 实现（{@code CustomPayloadEvent.Context} 已自带全部需要的信息）。
 */
final class ForgePayloadContext implements PacketContext {

    private final CustomPayloadEvent.Context context;

    ForgePayloadContext(CustomPayloadEvent.Context context) {
        this.context = context;
    }

    @Override
    public boolean isClientSide() {
        return context.isClientSide();
    }

    @Override
    public @Nullable ServerPlayer getSender() {
        return context.getSender();
    }

    @Override
    public Connection getConnection() {
        return context.getConnection();
    }

    @Override
    public void enqueueWork(Runnable runnable) {
        context.enqueueWork(runnable);
    }
}
