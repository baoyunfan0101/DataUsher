package com.datausher.data.datasource.api;

import java.util.Map;
import java.util.Objects;

public record DiscoveredDatasourceObject(
        String externalId,
        String parentExternalId,
        String name,
        DiscoveredObjectKind kind,
        Map<String, String> attributes
) {
    public DiscoveredDatasourceObject {
        externalId = requireText(externalId, "externalId");
        parentExternalId = parentExternalId == null ? "" : parentExternalId.trim();
        name = requireText(name, "name");
        kind = Objects.requireNonNull(kind, "kind must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (kind != DiscoveredObjectKind.DATABASE && parentExternalId.isEmpty()) {
            throw new IllegalArgumentException(
                    "parentExternalId is required for non-database objects");
        }
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
