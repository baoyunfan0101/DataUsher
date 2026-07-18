package com.datausher.data.metadata.api;

import com.datausher.data.datasource.api.DatasourceId;

import java.util.List;
import java.util.Optional;

public interface MetadataQueryService {
    Optional<CatalogMetadata> findCatalog(DatasourceId datasourceId);

    Optional<DatabaseMetadata> findDatabase(MetadataId databaseId);

    Optional<TableMetadata> findTable(MetadataId tableId);

    Optional<ColumnMetadata> findColumn(MetadataId columnId);

    List<DatabaseMetadata> listDatabases(MetadataId catalogId);

    List<TableMetadata> listTables(MetadataId databaseId);

    List<ColumnMetadata> listColumns(MetadataId tableId);
}
