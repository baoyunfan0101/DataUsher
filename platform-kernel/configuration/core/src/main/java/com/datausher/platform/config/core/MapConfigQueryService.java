package com.datausher.platform.config.core;

import com.datausher.platform.config.api.ConfigKey;
import com.datausher.platform.config.api.ConfigQueryService;
import com.datausher.platform.config.api.ConfigResolutionContext;
import com.datausher.platform.config.api.ConfigValue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class MapConfigQueryService implements ConfigQueryService {
    private final Map<String, String> values;
    private final String source;
    private final ConfigResolutionStrategy resolutionStrategy;

    public MapConfigQueryService(Map<String, String> values) {
        this(values, "map", ConfigResolutionStrategy.defaultStrategy());
    }

    public MapConfigQueryService(Map<String, String> values, String source) {
        this(values, source, ConfigResolutionStrategy.defaultStrategy());
    }

    public MapConfigQueryService(
            Map<String, String> values,
            String source,
            ConfigResolutionStrategy resolutionStrategy
    ) {
        this.values = validateValues(values);
        this.source = normalizeSource(source);
        this.resolutionStrategy = Objects.requireNonNull(resolutionStrategy, "resolutionStrategy must not be null");
    }

    @Override
    public Optional<ConfigValue> find(ConfigKey key) {
        return find(key, ConfigResolutionContext.GLOBAL);
    }

    @Override
    public Optional<ConfigValue> find(ConfigKey key, ConfigResolutionContext context) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(context, "context must not be null");
        for (String candidate : resolutionStrategy.candidates(key, context)) {
            String value = values.get(candidate);
            if (value != null) {
                return Optional.of(new ConfigValue(key, value, source + ":" + candidate));
            }
        }
        return Optional.empty();
    }

    private static Map<String, String> validateValues(Map<String, String> values) {
        Objects.requireNonNull(values, "values must not be null");
        Map<String, String> validated = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String candidate = Objects.requireNonNull(entry.getKey(), "configuration key must not be null");
            ConfigKey normalized = ConfigKey.of(candidate);
            if (!candidate.equals(normalized.name())) {
                throw new IllegalArgumentException("configuration key must use canonical form: " + candidate);
            }
            validated.put(candidate, Objects.requireNonNull(entry.getValue(), "configuration value must not be null"));
        }
        return Map.copyOf(validated);
    }

    private static String normalizeSource(String source) {
        String normalized = Objects.requireNonNull(source, "source must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("source must not be blank");
        }
        return normalized;
    }
}
