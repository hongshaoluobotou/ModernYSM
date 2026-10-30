package rip.ysm.api.impl;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Fabric 侧的 {@link rip.ysm.api.PlatformAPI} 实现。
 *
 * <p>各加载器模块提供同 FQN 同签名的实现类，common 只依赖这个「约定」而不依赖 fabric-api。
 */
public final class PlatformAPIImpl {

    private PlatformAPIImpl() {
    }

    public static boolean isServer() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
    }

    public static String getPlatformName() {
        return "Fabric";
    }

    public static Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Nullable
    public static String getModVersion(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse(null);
    }

    @Nullable
    public static String getModDisplayName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getName())
                .orElse(null);
    }

    public static Set<String> getLoadedModIds() {
        return FabricLoader.getInstance().getAllMods().stream()
                .map(ModContainer::getMetadata)
                .map(ModMetadata::getId)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Nullable
    public static Path getModRoot(String modId) {
        Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(modId);
        // getRootPaths 可能返回多个（jar + 资源包附加根），取第一个与 1.20.1 行为一致
        return container.flatMap(mod -> mod.getRootPaths().stream().findFirst()).orElse(null);
    }
}
