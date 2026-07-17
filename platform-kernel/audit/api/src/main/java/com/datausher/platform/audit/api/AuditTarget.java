package com.datausher.platform.audit.api;

import java.util.Objects;

public record AuditTarget(String resourceType, String resourceId, String scope) {
    public AuditTarget {
        resourceType = normalize(resourceType, "resourceType");
        resourceId = normalize(resourceId, "resourceId");
        scope = normalize(scope, "scope");
    }

    public static AuditTarget global(String resourceType, String resourceId) {
        return new AuditTarget(resourceType, resourceId, "global");
    }

    private static String normalize(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
