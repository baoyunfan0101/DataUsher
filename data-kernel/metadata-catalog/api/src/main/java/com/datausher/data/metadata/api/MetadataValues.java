package com.datausher.data.metadata.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

final class MetadataValues {
    private MetadataValues() {
    }

    static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    static String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }

    static Map<String, String> copyAttributes(Map<String, String> attributes) {
        return attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    static void validateAuditFields(Instant createdAt, Instant updatedAt, long revision) {
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be greater than zero");
        }
    }
}
