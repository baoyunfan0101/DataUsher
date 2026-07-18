package com.datausher.data.metadata.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record DatabaseMetadata(
        MetadataId databaseId,
        MetadataId catalogId,
        String name,
        String qualifiedName,
        Map<String, String> attributes,
        Instant createdAt,
        Instant updatedAt,
        long revision
) {
    public DatabaseMetadata {
        databaseId = Objects.requireNonNull(databaseId, "databaseId must not be null");
        catalogId = Objects.requireNonNull(catalogId, "catalogId must not be null");
        name = MetadataValues.requireText(name, "name");
        qualifiedName = MetadataValues.requireText(qualifiedName, "qualifiedName");
        attributes = MetadataValues.copyAttributes(attributes);
        MetadataValues.validateAuditFields(createdAt, updatedAt, revision);
    }
}
