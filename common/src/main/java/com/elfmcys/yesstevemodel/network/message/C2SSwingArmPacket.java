package com.elfmcys.yesstevemodel.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import rip.ysm.api.item.ToolActionBridge;
import rip.ysm.api.network.PacketContext;

public class C2SSwingArmPacket {

    private final InteractionHand hand;

    public C2SSwingArmPacket(InteractionHand hand) {
        this.hand = hand;
    }

    public static void encode(C2SSwingArmPacket message, FriendlyByteBuf buf) {
        buf.writeEnum(message.hand);
    }

    public static C2SSwingArmPacket decode(FriendlyByteBuf buf) {
        return new C2SSwingArmPacket(buf.readEnum(InteractionHand.class));
    }

    public static void handle(C2SSwingArmPacket message, PacketContext ctx) {
        ServerPlayer sender = ctx.getSender();
        if (ctx.isServerSide() && sender != null) {
            ctx.enqueueWork(() -> processSwingArm(message, sender));
        }
    }

    public static void processSwingArm(C2SSwingArmPacket message, ServerPlayer sender) {
        InteractionHand interactionHand = message.hand;
        ItemStack itemInHand = sender.getItemInHand(interactionHand);
        if (itemInHand.isEmpty() || !ToolActionBridge.onEntitySwing(itemInHand, sender)) {
            // 26.3 port（2026.11.02 复核）：与 1.20.1 的 "sender.swinging=false; sender.swing(hand)" 等价 ——
            // LivingEntity#swing(hand, animation, sendToSwingingEntity) 内部 SwingState.startIfAble 就是
            // 旧的守卫（正在挥动且 tick 未过半时忽略），广播由它自己发 ClientboundSwingAnimationPacket；
            // 第三参 false = 只发给追踪者、不回发本人（本人已在客户端预测挥动），与 1.20.1 一致。
            sender.swing(interactionHand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
        }
    }
}