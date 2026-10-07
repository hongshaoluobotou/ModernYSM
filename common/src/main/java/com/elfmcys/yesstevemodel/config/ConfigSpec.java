package com.elfmcys.yesstevemodel.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

import com.elfmcys.yesstevemodel.YesSteveModel;

/**
 * 轻量 TOML 配置层，替代原 Forge 的配置系统（26.3 移植）。
 * 对外保留与原 Forge 配置层相同的 Builder / 值类型 API（get/set），调用方代码无需改动。
 * 底层使用随模组提供的 Night Config；读取失败时保留原文件，本次会话只使用内存默认值。
 */
public final class ConfigSpec {

    private final CommentedConfig config;
    private final Path file;
    private final boolean canSave;

    private ConfigSpec(CommentedConfig config, Path file, boolean canSave) {
        this.config = config;
        this.file = file;
        this.canSave = canSave;
    }

    public synchronized void save() {
        // 解析失败后，build() 和后续 GUI set() 都不能覆盖待用户修复的配置。
        if (!canSave) {
            return;
        }
        Path temporary = null;
        try {
            Path target = file.toAbsolutePath();
            Files.createDirectories(target.getParent());
            String serialized = new TomlWriter().writeToString(config);
            temporary = Files.createTempFile(target.getParent(), ".ysm-config-", ".tmp");
            Files.writeString(temporary, serialized, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            YesSteveModel.LOGGER.error("Failed to save config file {}", file, e);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException e) {
                    YesSteveModel.LOGGER.warn("Failed to remove temporary config file {}", temporary, e);
                }
            }
        }
    }

    public static Builder builder(Path file) {
        return new Builder(file);
    }

    public static final class Builder {
        private final CommentedConfig config;
        private final Path file;
        private final boolean canSave;
        private final Deque<String> path = new ArrayDeque<>();
        private final List<String> comment = new ArrayList<>();
        private final List<AbstractValue<?>> values = new ArrayList<>();

        private Builder(Path file) {
            this.file = file;
            CommentedConfig loaded = null;
            boolean loadFailed = false;
            if (Files.isRegularFile(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    loaded = new TomlParser().parse(in, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    loadFailed = true;
                    YesSteveModel.LOGGER.error("Failed to load config file {}. Using in-memory defaults; " +
                            "the original file will be preserved. Fix it and restart before saving settings.", file, e);
                }
            }
            this.config = loaded != null ? loaded : CommentedConfig.inMemory();
            this.canSave = !loadFailed;
        }

        public Builder push(String name) {
            path.addLast(name);
            return this;
        }

        public Builder pop() {
            path.removeLast();
            return this;
        }

        public Builder comment(String... lines) {
            comment.addAll(Arrays.asList(lines));
            return this;
        }

        public BooleanValue define(String key, boolean defaultValue) {
            return register(new BooleanValue(key, defaultValue));
        }

        public IntValue defineInRange(String key, int defaultValue, int min, int max) {
            return register(new IntValue(key, defaultValue, min, max));
        }

        public DoubleValue defineInRange(String key, double defaultValue, double min, double max) {
            return register(new DoubleValue(key, defaultValue, min, max));
        }

        public <E extends Enum<E>> EnumValue<E> defineEnum(String key, E defaultValue) {
            return register(new EnumValue<>(key, defaultValue, defaultValue.getDeclaringClass()));
        }

        public StringValue define(String key, String defaultValue) {
            return register(new StringValue(key, defaultValue));
        }

        public StringListValue define(String key, List<String> defaultValue) {
            return register(new StringListValue(key, defaultValue));
        }

        private <T extends AbstractValue<?>> T register(T value) {
            String c = String.join("\n", comment);
            if (!c.isEmpty()) {
                value.comment = c;
            }
            comment.clear();
            value.attach(config, List.copyOf(path), value.key);
            values.add(value);
            return value;
        }

        public ConfigSpec build() {
            ConfigSpec spec = new ConfigSpec(config, file, canSave);
            for (AbstractValue<?> v : values) {
                v.setSpec(spec);
            }
            values.forEach(AbstractValue::get); // 强制补齐缺失键的默认值
            spec.save(); // 首次运行时把默认值写入文件，并保证注释存在
            return spec;
        }
    }

    public static abstract class AbstractValue<T> {
        final String key;
        final T defaultValue;
        private CommentedConfig config;
        private ConfigSpec spec;
        private List<String> path;
        String comment;
        private T cached;

        private AbstractValue(String key, T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        void attach(CommentedConfig config, List<String> parent, String key) {
            List<String> p = new ArrayList<>(parent);
            p.add(key);
            this.path = List.copyOf(p);
            this.config = config;
            if (comment != null && !comment.isEmpty()) {
                config.setComment(this.path, comment);
            }
        }

        void setSpec(ConfigSpec spec) {
            this.spec = spec;
        }

        private Object raw() {
            Object o = config.get(path);
            if (o == null) {
                config.set(path, serialize(defaultValue));
                return serialize(defaultValue);
            }
            return o;
        }

        public T get() {
            if (cached == null) {
                try {
                    cached = deserialize(raw());
                } catch (Exception e) {
                    cached = defaultValue;
                }
            }
            return cached;
        }

        public void set(T value) {
            cached = value;
            config.set(path, serialize(value));
            if (spec != null) {
                spec.save();
            }
        }

        protected Object serialize(T value) {
            return value;
        }

        protected abstract T deserialize(Object raw) throws Exception;
    }

    public static class BooleanValue extends AbstractValue<Boolean> {
        BooleanValue(String key, boolean defaultValue) {
            super(key, defaultValue);
        }

        @Override
        protected Boolean deserialize(Object raw) {
            if (raw instanceof Boolean b) return b;
            return defaultValue;
        }
    }

    public static class IntValue extends AbstractValue<Integer> {
        private final int min;
        private final int max;

        IntValue(String key, int defaultValue, int min, int max) {
            super(key, defaultValue);
            this.min = min;
            this.max = max;
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        @Override
        public Integer get() {
            return Math.clamp(super.get(), min, max);
        }

        @Override
        public void set(Integer value) {
            super.set(Math.clamp(value, min, max));
        }

        @Override
        protected Integer deserialize(Object raw) {
            if (raw instanceof Number n) return n.intValue();
            return defaultValue;
        }
    }

    public static class DoubleValue extends AbstractValue<Double> {
        private final double min;
        private final double max;

        DoubleValue(String key, double defaultValue, double min, double max) {
            super(key, defaultValue);
            this.min = min;
            this.max = max;
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

        @Override
        public Double get() {
            return Math.clamp(super.get(), min, max);
        }

        @Override
        public void set(Double value) {
            super.set(Math.clamp(value, min, max));
        }

        @Override
        protected Double deserialize(Object raw) {
            if (raw instanceof Number n) return n.doubleValue();
            return defaultValue;
        }
    }

    public static class StringValue extends AbstractValue<String> {
        StringValue(String key, String defaultValue) {
            super(key, defaultValue);
        }

        @Override
        protected String deserialize(Object raw) {
            if (raw instanceof String s) return s;
            return defaultValue;
        }
    }

    public static class StringListValue extends AbstractValue<List<String>> {
        StringListValue(String key, List<String> defaultValue) {
            super(key, defaultValue);
        }

        @Override
        protected Object serialize(List<String> value) {
            return new ArrayList<>(value);
        }

        @Override
        protected List<String> deserialize(Object raw) {
            if (raw instanceof List<?> list) {
                List<String> out = new ArrayList<>(list.size());
                for (Object o : list) {
                    out.add(String.valueOf(o));
                }
                return out;
            }
            return defaultValue;
        }
    }

    public static class EnumValue<E extends Enum<E>> extends AbstractValue<E> {
        private final Class<E> type;

        EnumValue(String key, E defaultValue, Class<E> type) {
            super(key, defaultValue);
            this.type = type;
        }

        @Override
        protected Object serialize(E value) {
            return value.name();
        }

        @Override
        protected E deserialize(Object raw) throws Exception {
            if (raw instanceof String s) {
                return Enum.valueOf(type, s.toUpperCase(Locale.ROOT));
            }
            return defaultValue;
        }
    }
}
