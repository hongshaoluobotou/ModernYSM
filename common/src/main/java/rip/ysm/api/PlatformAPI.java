package rip.ysm.api;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Set;

/**
 * 加载器平台信息 / 目录 / 模组查询的加载器无关入口。
 *
 * <p>每个加载器模块提供同签名的 {@code rip.ysm.api.impl.PlatformAPIImpl}：
 * <ul>
 *   <li>Fabric：{@code FabricLoader}</li>
 *   <li>NeoForge：{@code FMLPaths} / {@code ModList} / {@code FMLLoader#getDist}</li>
 *   <li>Forge：{@code FMLPaths} / {@code LoadingContext} / {@code FMLLoader#getDist}</li>
 * </ul>
 *
 * <p>约定：本类所有方法在加载器实现缺失时不得抛异常，未安装的模组查询返回 false/空。
 */
public final class PlatformAPI {

    private PlatformAPI() {
    }

    /** 物理端是否为专用服务器。 */
    public static boolean isServer() {
        return rip.ysm.api.impl.PlatformAPIImpl.isServer();
    }

    /** 加载器显示名（"Fabric" / "NeoForge" / "Forge"），用于日志与 UI。 */
    public static String getPlatformName() {
        return rip.ysm.api.impl.PlatformAPIImpl.getPlatformName();
    }

    /** 配置目录（Fabric：{@code FabricLoader#getConfigDir()}；NeoForge/Forge：{@code FMLPaths.CONFIGDIR}）。 */
    public static Path getConfigDir() {
        return rip.ysm.api.impl.PlatformAPIImpl.getConfigDir();
    }

    /**
     * 查询模组版本字符串。
     *
     * @return 版本号；模组未安装时返回 null
     */
    @Nullable
    public static String getModVersion(String modId) {
        return rip.ysm.api.impl.PlatformAPIImpl.getModVersion(modId);
    }

    /** 本模组自身的版本号（读不到时返回 "unknown"）。 */
    public static String getOwnVersion() {
        String version = getModVersion("yes_steve_model");
        return version == null ? "unknown" : version;
    }

    /**
     * 查询模组显示名（人类可读名称，用于调试输出）。
     *
     * @return 显示名；模组未安装时返回 null
     */
    @Nullable
    public static String getModDisplayName(String modId) {
        return rip.ysm.api.impl.PlatformAPIImpl.getModDisplayName(modId);
    }

    /** 指定模组 ID 是否已加载（包括本模组）。 */
    public static boolean isModLoaded(String modId) {
        return rip.ysm.api.impl.PlatformAPIImpl.isModLoaded(modId);
    }

    /** 已加载的全部模组 ID（用于 molang 的模组探测与日志）。 */
    public static Set<String> getLoadedModIds() {
        return rip.ysm.api.impl.PlatformAPIImpl.getLoadedModIds();
    }

    /**
     * 模组自身的资源根目录（jar 内文件系统的根，用于读内置 assets）。
     *
     * @return 目录路径；模组未安装时返回 null
     */
    @Nullable
    public static Path getModRoot(String modId) {
        return rip.ysm.api.impl.PlatformAPIImpl.getModRoot(modId);
    }
}
