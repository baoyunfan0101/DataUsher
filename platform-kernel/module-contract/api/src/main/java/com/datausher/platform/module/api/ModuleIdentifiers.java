package com.datausher.platform.module.api;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class ModuleIdentifiers {
    private static final Pattern IDENTIFIER_PATTERN =
            Pattern.compile("[a-z0-9][a-z0-9_-]*(\\.[a-z0-9][a-z0-9_-]*)*");

    private ModuleIdentifiers() {
    }

    public static String normalizeModuleName(String value) {
        return normalize(value, "moduleName");
    }

    static String normalizeCapabilityName(String value) {
        return normalize(value, "name");
    }

    private static String normalize(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (!IDENTIFIER_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(fieldName + " must be a canonical identifier");
        }
        return normalized;
    }
}
