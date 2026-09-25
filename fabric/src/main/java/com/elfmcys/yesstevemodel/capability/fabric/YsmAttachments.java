package com.elfmcys.yesstevemodel.capability.fabric;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.google.common.collect.MapMaker;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 自研附加数据存储（替代 Cardinal Components 的 ComponentKey / ComponentRegistryV3）。
 *
 * <p>每个组件对应一个静态 {@link Key}；数据用 weak-key Map 挂在实体上，实体被回收后自动释放。
 * 首次访问惰性创建并挂载，语义与原 {@code ComponentKey.getNullable} 一致
 * （原注册表中五个组件都注册了工厂，getNullable 实际总会返回实例）。</p>
 *
 * <p>持久化由 {@code EntityMixin} 完成：26.3 的 {@code Entity#saveWithoutId/load} 使用
 * ValueOutput/ValueInput，通过 accessor mixin 拿到底层 {@link CompoundTag} 后写入实体 NBT 的
 * {@code yes_steve_model} 子 tag 下（键名为组件短名）；读取时若该子 tag 不存在，
 * 回退到旧 Cardinal Components 的平铺键（{@code yes_steve_model:star_models} 等），兼容旧存档。</p>
 */
public final class YsmAttachments {

    public static final Key<StarModelsComponent> STAR_MODELS = new Key<>("star_models", StarModelsComponent::new, YsmAttachments::isPlayer);
    public static final Key<AuthModelsComponent> AUTH_MODELS = new Key<>("auth_models", AuthModelsComponent::new, YsmAttachments::isPlayer);
    public static final Key<ModelInfoComponent> MODEL_INFO = new Key<>("model_info", ModelInfoComponent::new, YsmAttachments::isPlayer);
    public static final Key<ProjectileModelComponent> PROJECTILE_MODEL = new Key<>("projectile_model", ProjectileModelComponent::new, Projectile.class::isInstance);
    public static final Key<VehicleModelComponent> VEHICLE_MODEL = new Key<>("vehicle_model", VehicleModelComponent::new, e -> true);

    private static final String ROOT_TAG = YesSteveModel.MOD_ID;
    private static final Key<?>[] KEYS = {STAR_MODELS, AUTH_MODELS, MODEL_INFO, PROJECTILE_MODEL, VEHICLE_MODEL};

    private static final Map<Entity, Map<Key<?>, YsmComponent>> ATTACHMENTS = new MapMaker().weakKeys().concurrencyLevel(2).makeMap();

    private YsmAttachments() {
    }

    private static boolean isPlayer(Entity entity) {
        return entity instanceof Player;
    }

    /** 语义与原 {@code ComponentKey.getNullable} 一致：不存在时惰性创建并挂载（原注册均带工厂，实际不会返回 null）。 */
    public static <T extends YsmComponent> T getNullable(Entity entity, Key<T> key) {
        if (!key.predicate.test(entity)) {
            return null;
        }
        Map<Key<?>, YsmComponent> map = ATTACHMENTS.get(entity);
        YsmComponent existing = map == null ? null : map.get(key);
        if (existing != null) {
            return key.cast(existing);
        }
        T created = key.factory.get();
        ATTACHMENTS.computeIfAbsent(entity, e -> new Object2ObjectOpenHashMap<>()).put(key, created);
        return created;
    }

    /** 玩家 respawn 复制（等价 CCA {@code RespawnCopyStrategy.ALWAYS_COPY}）：序列化旧组件 → 读入新实例。 */
    public static void copyAll(Entity from, Entity to) {
        for (Key<?> key : KEYS) {
            if (!key.predicate.test(to)) {
                continue;
            }
            Map<Key<?>, YsmComponent> map = ATTACHMENTS.get(from);
            YsmComponent oldComponent = map == null ? null : map.get(key);
            if (oldComponent == null) {
                continue;
            }
            CompoundTag tag = new CompoundTag();
            oldComponent.writeToNbt(tag);
            getNullable(to, key).readFromNbt(tag);
        }
    }

    /** 由 EntityMixin 在 {@code saveWithoutId} 末尾调用。 */
    public static void writeNbt(Entity entity, CompoundTag root) {
        Map<Key<?>, YsmComponent> map = ATTACHMENTS.get(entity);
        if (map == null || map.isEmpty()) {
            return;
        }
        for (Key<?> key : KEYS) {
            YsmComponent component = map.get(key);
            if (component != null) {
                CompoundTag tag = new CompoundTag();
                component.writeToNbt(tag);
                root.put(key.name, tag);
            }
        }
    }

    /** 由 EntityMixin 在 {@code load} 末尾调用；{@code legacyRoot} 为旧 CCA 平铺键所在的原始实体 NBT（可为 null）。 */
    public static void readNbt(Entity entity, CompoundTag root, CompoundTag legacyRoot) {
        for (Key<?> key : KEYS) {
            CompoundTag tag = null;
            if (root != null && root.contains(key.name)) {
                tag = root.getCompoundOrEmpty(key.name);
            } else if (legacyRoot != null && legacyRoot.contains(ROOT_TAG + ":" + key.name)) {
                tag = legacyRoot.getCompoundOrEmpty(ROOT_TAG + ":" + key.name);
            }
            if (tag != null && !tag.isEmpty()) {
                getNullable(entity, key).readFromNbt(tag);
            }
        }
    }

    public static final class Key<T extends YsmComponent> {
        final String name;
        final Supplier<T> factory;
        final Predicate<Entity> predicate;

        private Key(String name, Supplier<T> factory, Predicate<Entity> predicate) {
            this.name = name;
            this.factory = factory;
            this.predicate = predicate;
        }

        @SuppressWarnings("unchecked")
        T cast(YsmComponent component) {
            return (T) component;
        }
    }
}
