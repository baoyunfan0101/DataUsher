package com.datausher.data.metadata.api;

import java.util.Objects;

public record MetadataSyncResult(
        MetadataId catalogId,
        int created,
        int updated,
        int unchanged,
        int deleted,
        int schemaVersionsCreated
) {
    public MetadataSyncResult {
        catalogId = Objects.requireNonNull(catalogId, "catalogId must not be null");
        if (created < 0 || updated < 0 || unchanged < 0 || deleted < 0
                || schemaVersionsCreated < 0) {
            throw new IllegalArgumentException("synchronization counts must not be negative");
        }
    }
}
