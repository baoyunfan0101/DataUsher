package com.datausher.data.datasource.api;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record DatasourceDefinition(
        DatasourceId datasourceId,
        String displayName,
        String adapterId,
        String credentialBindingId,
        Map<String, String> connectionProperties,
        DatasourceStatus status,
        Instant createdAt,
        Instant updatedAt,
        long revision
) {
    public DatasourceDefinition {
        datasourceId = Objects.requireNonNull(datasourceId, "datasourceId must not be null");
        displayName = requireText(displayName, "displayName");
        adapterId = normalizeIdentifier(adapterId, "adapterId");
        credentialBindingId = normalizeIdentifier(credentialBindingId, "credentialBindingId");
        connectionProperties = validateProperties(connectionProperties);
        status = Objects.requireNonNull(status, "status must not be null");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be greater than zero");
        }
    }

    public DatasourceDefinition withStatus(DatasourceStatus nextStatus, Instant changedAt) {
        return new DatasourceDefinition(
                datasourceId,
                displayName,
                adapterId,
                credentialBindingId,
                connectionProperties,
                Objects.requireNonNull(nextStatus, "nextStatus must not be null"),
                createdAt,
                Objects.requireNonNull(changedAt, "changedAt must not be null"),
                revision + 1
        );
    }

    private static Map<String, String> validateProperties(Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) {
            return Map.of();
        }
        Map<String, String> validated = new LinkedHashMap<>();
        properties.forEach((key, value) -> {
            String normalizedKey = normalizePropertyKey(key);
            if (containsSensitiveName(normalizedKey)) {
                throw new IllegalArgumentException(
                        "connectionProperties must not contain credentials: " + normalizedKey);
            }
            String normalizedValue = requireText(value, "connectionProperties value");
            if (validated.putIfAbsent(normalizedKey, normalizedValue) != null) {
                throw new IllegalArgumentException(
                        "duplicate connection property: " + normalizedKey);
            }
        });
        return Map.copyOf(validated);
    }

    private static boolean containsSensitiveName(String key) {
        return key.matches(".*(password|passwd|secret|token|credential|private[._-]?key).*");
    }

    private static String normalizePropertyKey(String value) {
        String normalized = normalizeIdentifier(value, "connectionProperties key");
        if (!normalized.matches("[a-z][a-z0-9._-]{0,126}")) {
            throw new IllegalArgumentException("invalid connection property key: " + value);
        }
        return normalized;
    }

    private static String normalizeIdentifier(String value, String fieldName) {
        String normalized = requireText(value, fieldName).toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z][a-z0-9._-]{0,126}")) {
            throw new IllegalArgumentException(
                    fieldName + " must match [a-z][a-z0-9._-]{0,126}");
        }
        return normalized;
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
