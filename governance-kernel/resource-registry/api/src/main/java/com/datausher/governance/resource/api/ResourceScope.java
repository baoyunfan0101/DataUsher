package com.datausher.governance.resource.api;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public record ResourceScope(ResourceScopeType type, String projectId, String environmentId) {
    public ResourceScope {
        type = Objects.requireNonNull(type, "type must not be null");
        projectId = normalizeNullable(projectId);
        environmentId = normalizeNullable(environmentId);
        if (type == ResourceScopeType.GLOBAL && (projectId != null || environmentId != null)) {
            throw new IllegalArgumentException("global scope must not contain project or environment IDs");
        }
        if (type == ResourceScopeType.PROJECT && (projectId == null || environmentId != null)) {
            throw new IllegalArgumentException("project scope requires only a project ID");
        }
        if (type == ResourceScopeType.ENVIRONMENT && (projectId == null || environmentId == null)) {
            throw new IllegalArgumentException("environment scope requires project and environment IDs");
        }
    }

    public static ResourceScope global() {
        return new ResourceScope(ResourceScopeType.GLOBAL, null, null);
    }

    public static ResourceScope project(String projectId) {
        return new ResourceScope(ResourceScopeType.PROJECT, projectId, null);
    }

    public static ResourceScope environment(String projectId, String environmentId) {
        return new ResourceScope(ResourceScopeType.ENVIRONMENT, projectId, environmentId);
    }

    public String canonicalValue() {
        return switch (type) {
            case GLOBAL -> "global";
            case PROJECT -> "project/" + encode(projectId);
            case ENVIRONMENT -> "environment/" + encode(projectId) + "/" + encode(environmentId);
        };
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("scope IDs must not be blank");
        }
        return normalized;
    }
}
