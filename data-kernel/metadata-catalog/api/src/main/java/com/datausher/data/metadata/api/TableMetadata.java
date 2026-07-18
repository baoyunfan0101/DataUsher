package com.datausher.data.metadata.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record TableMetadata(
        MetadataId tableId,
        MetadataId databaseId,
        String name,
        String qualifiedName,
        TableKind kind,
        String description,
        Map<String, String> attributes,
        Instant createdAt,
        Instant updatedAt,
        long revision
) {
    public TableMetadata {
        tableId = Objects.requireNonNull(tableId, "tableId must not be null");
        databaseId = Objects.requireNonNull(databaseId, "databaseId must not be null");
        name = MetadataValues.requireText(name, "name");
        qualifiedName = MetadataValues.requireText(qualifiedName, "qualifiedName");
        kind = Objects.requireNonNull(kind, "kind must not be null");
        description = MetadataValues.normalizeOptional(description);
        attributes = MetadataValues.copyAttributes(attributes);
        MetadataValues.validateAuditFields(createdAt, updatedAt, revision);
    }
}
