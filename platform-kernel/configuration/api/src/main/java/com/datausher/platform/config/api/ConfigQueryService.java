package com.datausher.platform.config.api;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public interface ConfigQueryService {
    Optional<ConfigValue> find(ConfigKey key);

    Optional<ConfigValue> find(ConfigKey key, ConfigResolutionContext context);

    default Optional<String> getString(ConfigKey key) {
        return find(key).map(ConfigValue::value);
    }

    default Optional<String> getString(ConfigKey key, ConfigResolutionContext context) {
        return find(key, context).map(ConfigValue::value);
    }

    default String getString(ConfigKey key, String defaultValue) {
        return getString(key).orElse(Objects.requireNonNull(defaultValue, "defaultValue must not be null"));
    }

    default String getString(ConfigKey key, ConfigResolutionContext context, String defaultValue) {
        return getString(key, context).orElse(Objects.requireNonNull(defaultValue, "defaultValue must not be null"));
    }

    default boolean getBoolean(ConfigKey key, boolean defaultValue) {
        return find(key).map(ConfigValue::asBoolean).orElse(defaultValue);
    }

    default boolean getBoolean(ConfigKey key, ConfigResolutionContext context, boolean defaultValue) {
        return find(key, context).map(ConfigValue::asBoolean).orElse(defaultValue);
    }

    default int getInt(ConfigKey key, int defaultValue) {
        return find(key).map(ConfigValue::asInt).orElse(defaultValue);
    }

    default int getInt(ConfigKey key, ConfigResolutionContext context, int defaultValue) {
        return find(key, context).map(ConfigValue::asInt).orElse(defaultValue);
    }

    default long getLong(ConfigKey key, long defaultValue) {
        return find(key).map(ConfigValue::asLong).orElse(defaultValue);
    }

    default long getLong(ConfigKey key, ConfigResolutionContext context, long defaultValue) {
        return find(key, context).map(ConfigValue::asLong).orElse(defaultValue);
    }

    default Duration getDuration(ConfigKey key, Duration defaultValue) {
        return find(key).map(ConfigValue::asDuration)
                .orElse(Objects.requireNonNull(defaultValue, "defaultValue must not be null"));
    }

    default Duration getDuration(ConfigKey key, ConfigResolutionContext context, Duration defaultValue) {
        return find(key, context).map(ConfigValue::asDuration)
                .orElse(Objects.requireNonNull(defaultValue, "defaultValue must not be null"));
    }
}
