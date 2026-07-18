package com.datausher.data.metadata.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record ColumnMetadata(
        MetadataId columnId,
        MetadataId tableId,
        String name,
        String qualifiedName,
        int ordinalPosition,
        String dataType,
        boolean nullable,
        String description,
        Map<String, String> attributes,
        Instant createdAt,
        Instant updatedAt,
        long revision
) {
    public ColumnMetadata {
        columnId = Objects.requireNonNull(columnId, "columnId must not be null");
        tableId = Objects.requireNonNull(tableId, "tableId must not be null");
        name = MetadataValues.requireText(name, "name");
        qualifiedName = MetadataValues.requireText(qualifiedName, "qualifiedName");
        dataType = MetadataValues.requireText(dataType, "dataType");
        description = MetadataValues.normalizeOptional(description);
        attributes = MetadataValues.copyAttributes(attributes);
        MetadataValues.validateAuditFields(createdAt, updatedAt, revision);
        if (ordinalPosition < 1) {
            throw new IllegalArgumentException("ordinalPosition must be greater than zero");
        }
    }
}
