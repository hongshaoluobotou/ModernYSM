package rip.ysm.api.impl;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge 侧的 {@link rip.ysm.api.PlatformAPI} 实现。
 *
 * <p>与 NeoForge 版基本同构（{@code FMLEnvironment}/{@code FMLPaths}/{@code ModList} 名字一致），
 * 唯一差异是包名（{@code net.minecraftforge.*}）与 mod 文件 API：
 * Forge 的 {@link IModFile#findResource(String...)} 直接给出资源路径，
 * 不再需要遍历 NeoForge 的 {@code JarContents#getContentRoots()}。
 */
public final class PlatformAPIImpl {

    /** 模组 jar 的 zip 文件系统缓存（{@link #getModRoot} 用）。 */
    private static final Map<Path, Path> MOD_ROOTS = new ConcurrentHashMap<>();

    private PlatformAPIImpl() {
    }

    public static boolean isServer() {
        return FMLEnvironment.dist.isDedicatedServer();
    }

    public static String getPlatformName() {
        return "Forge";
    }

    public static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static boolean isModLoaded(String modId) {
        return ModList.isLoaded(modId);
    }

    @Nullable
    public static String getModVersion(String modId) {
        return ModList.getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse(null);
    }

    @Nullable
    public static String getModDisplayName(String modId) {
        return ModList.getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(null);
    }

    public static Set<String> getLoadedModIds() {
        Set<String> ids = new TreeSet<>();
        for (IModInfo info : ModList.getMods()) {
            ids.add(info.getModId());
        }
        return ids;
    }

    /**
     * 返回模组资源根目录（jar 内走 zipfs，开发环境返回 classes/resources 目录）。
     *
     * <p>{@code findResource("assets")} 会返回 {@code <root>/assets}（dev 下是真实目录，
     * 生产环境是 zipfs 路径），取其父目录即模组根。</p>
     */
    @Nullable
    public static Path getModRoot(String modId) {
        IModFileInfo info = ModList.getModFileById(modId);
        if (info == null) {
            return null;
        }
        IModFile file = info.getFile();
        try {
            Path assets = file.findResource("assets");
            if (assets != null && assets.getParent() != null) {
                return assets.getParent();
            }
        } catch (Exception ignored) {
            // findResource 在目录不存在时可能抛异常，退回下面的路径逻辑
        }

        Path path = file.getFilePath();
        if (path == null) {
            return null;
        }
        if (Files.isDirectory(path)) {
            return path;
        }
        return MOD_ROOTS.computeIfAbsent(path, PlatformAPIImpl::openZipRoot);
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
