package com.datausher.execution.api;

import java.util.Map;
import java.util.Objects;

public record ExecutionOrigin(
        ExecutionOriginType type,
        String originId,
        String attemptKey,
        Map<String, String> attributes
) {
    public ExecutionOrigin {
        type = Objects.requireNonNull(type, "type must not be null");
        originId = requireText(originId, "originId");
        attemptKey = requireText(attemptKey, "attemptKey");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static ExecutionOrigin direct(String originId) {
        return new ExecutionOrigin(ExecutionOriginType.DIRECT, originId, "1", Map.of());
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
