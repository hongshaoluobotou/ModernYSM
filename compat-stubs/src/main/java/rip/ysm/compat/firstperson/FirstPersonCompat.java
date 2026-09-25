package rip.ysm.compat.firstperson;

// TODO port: stub, 原实现依赖 1.20.1 的 mod jar，恢复 compat 时替换（真实现见 common/src/main/java/rip/ysm/compat，被构建排除）


public final class FirstPersonCompat {

    private FirstPersonCompat() {
    }

    
    public static boolean isLoaded() {
        return false;
    }

    
    public static boolean isFirstPersonActive() {
        return false;
    }

    
    public static boolean shouldHideHead() {
        return false;
    }

    
    public static void setCameraDistance(float distance) {
        {}
    }
}
