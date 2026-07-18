package com.datausher.data.metadata.core;

import com.datausher.data.metadata.api.CatalogMetadata;
import com.datausher.data.metadata.api.ColumnMetadata;
import com.datausher.data.metadata.api.DatabaseMetadata;
import com.datausher.data.metadata.api.MetadataId;
import com.datausher.data.metadata.api.TableMetadata;
import com.datausher.data.metadata.api.TableSchema;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record MetadataSyncBatch(
        CatalogMetadata catalog,
        List<DatabaseMetadata> databases,
        List<TableMetadata> tables,
        List<ColumnMetadata> columns,
        Map<MetadataId, TableSchema> schemas
) {
    public MetadataSyncBatch {
        catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        databases = List.copyOf(databases);
        tables = List.copyOf(tables);
        columns = List.copyOf(columns);
        schemas = Map.copyOf(schemas);
    }
}
