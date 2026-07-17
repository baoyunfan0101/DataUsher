package com.datausher.governance.project.api;

import java.util.Objects;

public record EnvironmentSpec(String key, String displayName) {
    public EnvironmentSpec {
        key = normalizeKey(key);
        displayName = normalize(displayName, "displayName");
    }

    private static String normalizeKey(String value) {
        String normalized = normalize(value, "key").toLowerCase();
        if (!normalized.matches("[a-z][a-z0-9-]{0,62}")) {
            throw new IllegalArgumentException("key must match [a-z][a-z0-9-]{0,62}");
        }
        return normalized;
    }

    private static String normalize(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
