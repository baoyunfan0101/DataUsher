package com.datausher.governance.project.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record Project(
        String projectId,
        String key,
        String displayName,
        ProjectStatus status,
        Instant createdAt,
        String createdBy,
        Map<String, String> attributes
) {
    public Project {
        projectId = normalize(projectId, "projectId");
        key = normalize(key, "key");
        displayName = normalize(displayName, "displayName");
        status = Objects.requireNonNull(status, "status must not be null");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        createdBy = normalize(createdBy, "createdBy");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public Project withStatus(ProjectStatus nextStatus) {
        return new Project(projectId, key, displayName, nextStatus, createdAt, createdBy, attributes);
    }

    private static String normalize(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
