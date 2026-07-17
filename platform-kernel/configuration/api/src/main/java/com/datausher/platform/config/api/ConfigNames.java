package com.datausher.platform.config.api;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

final class ConfigNames {
    private static final Pattern SEGMENT_PATTERN = Pattern.compile("[a-z0-9][a-z0-9_-]*");
    private static final Pattern KEY_PATTERN =
            Pattern.compile("[a-z0-9][a-z0-9_-]*(\\.[a-z0-9][a-z0-9_-]*)*");

    private ConfigNames() {
    }

    static String normalizeKey(String value) {
        return normalize(value, KEY_PATTERN, "name must contain canonical dot-separated segments");
    }

    static String normalizeSegment(String value) {
        return normalize(value, SEGMENT_PATTERN, "name must be a canonical configuration segment");
    }

    private static String normalize(String value, Pattern pattern, String invalidMessage) {
        String normalized = Objects.requireNonNull(value, "name must not be null")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (!pattern.matcher(normalized).matches()) {
            throw new IllegalArgumentException(invalidMessage);
        }
        return normalized;
    }
}
