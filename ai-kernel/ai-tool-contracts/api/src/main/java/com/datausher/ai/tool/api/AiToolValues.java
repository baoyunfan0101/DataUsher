package com.datausher.ai.tool.api;

import java.util.Map;
import java.util.Objects;

final class AiToolValues {
    private AiToolValues() {
    }

    static String id(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null")
                .trim().toLowerCase();
        if (!normalized.matches("[a-z][a-z0-9.-]{0,126}")) {
            throw new IllegalArgumentException(
                    fieldName + " must match [a-z][a-z0-9.-]{0,126}");
        }
        return normalized;
    }

    static String text(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    static String optionalText(String value) {
        return value == null ? "" : value.trim();
    }

    static Map<String, String> attributes(Map<String, String> attributes) {
        return attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
