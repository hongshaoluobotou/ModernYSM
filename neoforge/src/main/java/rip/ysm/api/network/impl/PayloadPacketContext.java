package rip.ysm.api.network.impl;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;
import rip.ysm.api.network.PacketContext;

/**
 * NeoForge 侧的 {@link PacketContext} 实现。
 *
 * <p>fabric 版按方向分了 {@code ServerPacketContext} / {@code ClientPacketContext} 两个类（各自的
 * sender/线程池来源不同）；NeoForge 的 {@code IPayloadContext} 已同时提供 player/connection/enqueueWork，
 * 故用同一个类、以 {@code clientSide} 标记区分方向。
 */
public final class PayloadPacketContext implements PacketContext {

    private final IPayloadContext context;
    private final boolean clientSide;

    public PayloadPacketContext(IPayloadContext context, boolean clientSide) {
        this.context = context;
        this.clientSide = clientSide;
    }

    @Override
    public boolean isClientSide() {
        return clientSide;
    }

    @Override
    public @Nullable ServerPlayer getSender() {
        return context.player() instanceof ServerPlayer player ? player : null;
    }

    @Override
    public Connection getConnection() {
        return context.connection();
    }

    @Override
    public void enqueueWork(Runnable runnable) {
        context.enqueueWork(runnable);
    }
}
