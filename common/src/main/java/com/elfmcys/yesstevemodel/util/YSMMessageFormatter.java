package com.elfmcys.yesstevemodel.util;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import com.elfmcys.yesstevemodel.util.ServerInstanceHolder;
import org.jetbrains.annotations.Nullable;
import rip.ysm.api.PlatformAPI;

public class YSMMessageFormatter {

    private static final String PREFIX = "§6§l【§aYSM§6§l】§r";

    public static Component withPrefix(Component component) {
        return Component.literal(PREFIX).append(component);
    }

    public static boolean isCurrentClientPlayer(Entity entity) {
        return entity != null && !PlatformAPI.isServer() && entity.getUUID().equals(Minecraft.getInstance().getUser().getProfileId());
    }

    public static boolean hasPermission(@Nullable Entity entity, int level) {
        if (entity == null) {
            return false;
        }
        return com.elfmcys.yesstevemodel.util.PermissionsCompat.hasPermission(entity instanceof net.minecraft.server.level.ServerPlayer serverPlayer ? serverPlayer.permissions() : net.minecraft.server.permissions.PermissionSet.NO_PERMISSIONS, level) || isCurrentClientPlayer(entity);
    }

    public static boolean hasCommandPermission(CommandSourceStack commandSourceStack, int level) {
        if (com.elfmcys.yesstevemodel.util.PermissionsCompat.hasPermission(commandSourceStack, level)) {
            return true;
        }
        return commandSourceStack.getEntity() != null && isCurrentClientPlayer(commandSourceStack.getEntity());
    }

    public static void sendServerMessage(@Nullable CommandSourceStack commandSourceStack, Component component, boolean broadcastToOps) {
        MinecraftServer currentServer = ServerInstanceHolder.getServer();
        if (currentServer == null) {
            return;
        }
        currentServer.execute(() -> {
            ServerPlayer player;
            CommandSourceStack sourceStack = null;
            if (commandSourceStack != null && (commandSourceStack.getEntity() instanceof ServerPlayer) && (player = currentServer.getPlayerList().getPlayer(commandSourceStack.getEntity().getUUID())) != null) {
                sourceStack = player.createCommandSourceStack();
            }
            if (sourceStack == null) {
                sourceStack = currentServer.createCommandSourceStack();
            }
            sourceStack.sendSuccess(() -> component, broadcastToOps);
        });
    }
}