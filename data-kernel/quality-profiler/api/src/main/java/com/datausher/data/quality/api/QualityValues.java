package com.datausher.data.quality.api;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class QualityValues {
    private QualityValues() {
    }

    static String id(String value, String fieldName) {
        String normalized = text(value, fieldName).toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z][a-z0-9._-]{0,254}")) {
            throw new IllegalArgumentException(
                    fieldName + " must match [a-z][a-z0-9._-]{0,254}");
        }
        return normalized;
    }

    static String text(String value, String fieldName) {
        String normalized = Objects.requireNonNull(
                value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    static Optional<String> optional(Optional<String> value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        String normalized = value.orElseThrow().trim();
        return normalized.isEmpty() ? Optional.empty() : Optional.of(normalized);
    }

    static Map<String, String> attributes(Map<String, String> values) {
        return values == null ? Map.of() : Map.copyOf(values);
    }
}
