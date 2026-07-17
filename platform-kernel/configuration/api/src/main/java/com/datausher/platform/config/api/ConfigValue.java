package com.datausher.platform.config.api;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;

public record ConfigValue(ConfigKey key, String value, String source) {
    public ConfigValue {
        key = Objects.requireNonNull(key, "key must not be null");
        value = Objects.requireNonNull(value, "value must not be null");
        source = Objects.requireNonNull(source, "source must not be null").trim();
        if (source.isEmpty()) {
            throw new IllegalArgumentException("source must not be blank");
        }
    }

    public boolean asBoolean() {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw conversionFailure("boolean", null);
        };
    }

    public int asInt() {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException failure) {
            throw conversionFailure("int", failure);
        }
    }

    public long asLong() {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException failure) {
            throw conversionFailure("long", failure);
        }
    }

    public Duration asDuration() {
        try {
            return Duration.parse(value.trim());
        } catch (DateTimeParseException failure) {
            throw conversionFailure("duration", failure);
        }
    }

    private IllegalArgumentException conversionFailure(String targetType, RuntimeException cause) {
        String message = "configuration " + key.name() + " from " + source
                + " is not a valid " + targetType;
        return cause == null ? new IllegalArgumentException(message) : new IllegalArgumentException(message, cause);
    }
}
