package com.datausher.data.datasource.api;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record DatasourceDiscoverySnapshot(
        String discoveryId,
        DatasourceId datasourceId,
        String namespace,
        List<DiscoveredDatasourceObject> objects,
        Instant discoveredAt
) {
    public DatasourceDiscoverySnapshot {
        discoveryId = requireText(discoveryId, "discoveryId");
        datasourceId = Objects.requireNonNull(datasourceId, "datasourceId must not be null");
        namespace = namespace == null ? "" : namespace.trim();
        objects = objects == null ? List.of() : List.copyOf(objects);
        discoveredAt = Objects.requireNonNull(discoveredAt, "discoveredAt must not be null");
        HashSet<String> externalIds = new HashSet<>();
        for (DiscoveredDatasourceObject object : objects) {
            if (!externalIds.add(object.externalId())) {
                throw new IllegalArgumentException(
                        "duplicate discovered externalId: " + object.externalId());
            }
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
