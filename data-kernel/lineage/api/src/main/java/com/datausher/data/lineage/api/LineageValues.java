package com.datausher.data.lineage.api;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class LineageValues {
    private LineageValues() {
    }

    static String identifier(String value, String fieldName) {
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

    static Map<String, String> attributes(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(values);
    }
}
