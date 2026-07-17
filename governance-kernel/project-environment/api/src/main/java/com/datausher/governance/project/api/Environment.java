package com.datausher.governance.project.api;

import java.time.Instant;
import java.util.Objects;

public record Environment(
        String environmentId,
        String projectId,
        String key,
        String displayName,
        EnvironmentStatus status,
        Instant createdAt
) {
    public Environment {
        environmentId = normalize(environmentId, "environmentId");
        projectId = normalize(projectId, "projectId");
        key = normalize(key, "key");
        displayName = normalize(displayName, "displayName");
        status = Objects.requireNonNull(status, "status must not be null");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    private static String normalize(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
