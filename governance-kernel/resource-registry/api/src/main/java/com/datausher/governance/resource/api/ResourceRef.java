package com.datausher.governance.resource.api;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public record ResourceRef(String resourceType, String resourceId, ResourceScope scope) {
    public ResourceRef {
        resourceType = normalizeType(resourceType);
        resourceId = normalize(resourceId, "resourceId");
        scope = Objects.requireNonNull(scope, "scope must not be null");
    }

    public static ResourceRef global(String resourceType, String resourceId) {
        return new ResourceRef(resourceType, resourceId, ResourceScope.global());
    }

    public String canonicalValue() {
        return resourceType + ":" + scope.canonicalValue() + ":" + encode(resourceId);
    }

    private static String normalizeType(String value) {
        String normalized = normalize(value, "resourceType").toLowerCase();
        if (!normalized.matches("[a-z][a-z0-9.-]{0,126}")) {
            throw new IllegalArgumentException("resourceType must match [a-z][a-z0-9.-]{0,126}");
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

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
