package rip.ysm.api.env;

/**
 * 运行环境枚举（替代 Fabric API 的 {@code net.fabricmc.api.EnvType}）。
 *
 * <p>语义与 Fabric 版一致：只描述「这段代码属于哪一侧」。
 */
public enum EnvType {
    CLIENT,
    SERVER
}
