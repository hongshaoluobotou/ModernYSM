package rip.ysm.compat.oculus;

// TODO port: stub, 原实现依赖 1.20.1 的 mod jar，恢复 compat 时替换（真实现见 common/src/main/java/rip/ysm/compat，被构建排除）


public final class OculusCompat {

    private OculusCompat() {
    }

    
    public static boolean isLoaded() {
        return false;
    }

    
    public static boolean isPBRActive() {
        return false;
    }

    
    public static void updatePBRState() {
        {}
    }

    
    public static boolean isShaderPackInUse() {
        return false;
    }

    
    public static boolean isRenderingShadowPass() {
        return false;
    }
}
