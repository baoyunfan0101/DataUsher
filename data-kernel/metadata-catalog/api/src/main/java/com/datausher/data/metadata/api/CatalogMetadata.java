package com.datausher.data.metadata.api;

import com.datausher.data.datasource.api.DatasourceId;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record CatalogMetadata(
        MetadataId catalogId,
        DatasourceId datasourceId,
        String name,
        Map<String, String> attributes,
        Instant createdAt,
        Instant updatedAt,
        long revision
) {
    public CatalogMetadata {
        catalogId = Objects.requireNonNull(catalogId, "catalogId must not be null");
        datasourceId = Objects.requireNonNull(datasourceId, "datasourceId must not be null");
        name = MetadataValues.requireText(name, "name");
        attributes = MetadataValues.copyAttributes(attributes);
        MetadataValues.validateAuditFields(createdAt, updatedAt, revision);
    }
}
