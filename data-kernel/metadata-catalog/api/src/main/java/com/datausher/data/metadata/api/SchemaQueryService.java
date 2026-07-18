package com.datausher.data.metadata.api;

import java.util.List;
import java.util.Optional;

public interface SchemaQueryService {
    Optional<TableSchema> findCurrentSchema(MetadataId tableId);

    List<TableSchema> listSchemaVersions(MetadataId tableId);
}
