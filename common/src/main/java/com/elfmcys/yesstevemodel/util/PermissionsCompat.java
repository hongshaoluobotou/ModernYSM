package com.elfmcys.yesstevemodel.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;

// TODO port: 26.3 权限系统改为 PermissionSet/PermissionLevel，旧 hasPermission(int) 语义在此近似
public class PermissionsCompat {

    private PermissionsCompat() {
    }

    public static boolean hasPermission(PermissionSet permissions, int level) {
        int clamped = Math.max(0, Math.min(4, level));
        PermissionLevel permissionLevel = PermissionLevel.values()[clamped];
        return permissions.hasPermission(new Permission.HasCommandLevel(permissionLevel));
    }

    public static boolean hasPermission(CommandSourceStack source, int level) {
        return hasPermission(source.permissions(), level);
    }
}
