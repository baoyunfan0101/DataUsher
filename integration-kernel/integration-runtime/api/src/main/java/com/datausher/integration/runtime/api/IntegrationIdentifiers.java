package com.datausher.integration.runtime.api;

import java.util.Locale;
import java.util.Objects;

public final class IntegrationIdentifiers {
    private IntegrationIdentifiers() {
    }

    public static String normalize(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z][a-z0-9._-]{0,126}")) {
            throw new IllegalArgumentException(
                    fieldName + " must match [a-z][a-z0-9._-]{0,126}");
        }
        return normalized;
    }

    public static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
