package rip.ysm.api.impl;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NeoForge 侧的 {@link rip.ysm.api.PlatformAPI} 实现。
 *
 * <p>与 fabric 版同 FQN 同签名，common 只依赖这个「约定」。
 * 对应关系：{@code FabricLoader#getEnvironmentType} → {@code FMLEnvironment#getDist}、
 * {@code FabricLoader#getConfigDir} → {@code FMLPaths.CONFIGDIR}、
 * {@code FabricLoader#getAllMods} → {@code ModList#getMods}。
 */
public final class PlatformAPIImpl {

    /** 模组 jar 的 zip 文件系统缓存（{@link #getModRoot} 用，zipfs 创建有成本且不可频繁关闭）。 */
    private static final Map<Path, Path> MOD_ROOTS = new ConcurrentHashMap<>();

    private PlatformAPIImpl() {
    }

    public static boolean isServer() {
        return FMLEnvironment.getDist().isDedicatedServer();
    }

    public static String getPlatformName() {
        return "NeoForge";
    }

    public static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Nullable
    public static String getModVersion(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse(null);
    }

    @Nullable
    public static String getModDisplayName(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(null);
    }

    public static Set<String> getLoadedModIds() {
        Set<String> ids = new TreeSet<>();
        for (IModInfo info : ModList.get().getMods()) {
            ids.add(info.getModId());
        }
        return ids;
    }

    /**
     * 返回模组资源根目录（jar 内走 zipfs，开发环境返回 ModDevGradle 的资源文件夹）。
     *
     * <p>开发环境的 mod 内容是“复合”的（classes 目录 + resources 目录，见 FML
     * {@code CompositeJarContents}），{@code IModFile#getFilePath()} 只给出其中一个，
     * 故优先遍历 {@code getContentRoots()} 并挑选含 {@code assets/} 的那一个。</p>
     */
    @Nullable
    public static Path getModRoot(String modId) {
        IModFileInfo info = ModList.get().getModFileById(modId);
        if (info == null) {
            return null;
        }
        IModFile file = info.getFile();
        Collection<Path> roots = file.getContents().getContentRoots();

        // 开发环境：优先返回含 assets 的目录根
        for (Path root : roots) {
            if (Files.isDirectory(root) && Files.isDirectory(root.resolve("assets"))) {
                return root;
            }
        }
        for (Path root : roots) {
            if (Files.isDirectory(root)) {
                return root;
            }
        }

        // 生产环境：内容根就是 jar 文件 → 打开 zipfs
        Path path = file.getContents().getPrimaryPath();
        if (path == null) {
            path = file.getFilePath();
        }
        if (path == null) {
            return null;
        }
        if (Files.isDirectory(path)) {
            return path;
        }
        Path jar = path;
        return MOD_ROOTS.computeIfAbsent(jar, PlatformAPIImpl::openZipRoot);
    }

    @Nullable
    private static Path openZipRoot(Path jar) {
        try {
            return FileSystems.getFileSystem(URI.create("jar:" + jar.toUri())).getPath("/");
        } catch (Exception ignored) {
            // 未创建过该 jar 的文件系统
        }
        try {
            FileSystem fs = FileSystems.newFileSystem(jar, Map.of());
            return fs.getPath("/");
        } catch (IOException | RuntimeException e) {
            com.elfmcys.yesstevemodel.YesSteveModel.LOGGER.error("Failed to open mod jar as file system: " + jar, e);
            return null;
        }
    }
}
