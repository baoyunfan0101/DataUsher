package com.datausher.data.metadata.core;

import com.datausher.data.datasource.api.DatasourceId;
import com.datausher.data.metadata.api.CatalogMetadata;
import com.datausher.data.metadata.api.ColumnMetadata;
import com.datausher.data.metadata.api.DatabaseMetadata;
import com.datausher.data.metadata.api.MetadataId;
import com.datausher.data.metadata.api.MetadataSyncMode;
import com.datausher.data.metadata.api.MetadataSyncResult;
import com.datausher.data.metadata.api.TableMetadata;
import com.datausher.data.metadata.api.TableSchema;

import java.util.List;
import java.util.Optional;

public interface MetadataStore {
    MetadataSyncResult synchronize(MetadataSyncBatch batch, MetadataSyncMode mode);

    Optional<CatalogMetadata> findCatalog(DatasourceId datasourceId);

    Optional<DatabaseMetadata> findDatabase(MetadataId databaseId);

    Optional<TableMetadata> findTable(MetadataId tableId);

    Optional<ColumnMetadata> findColumn(MetadataId columnId);

    List<DatabaseMetadata> listDatabases(MetadataId catalogId);

    List<TableMetadata> listTables(MetadataId databaseId);

    List<ColumnMetadata> listColumns(MetadataId tableId);

    Optional<TableSchema> findCurrentSchema(MetadataId tableId);

    List<TableSchema> listSchemaVersions(MetadataId tableId);

    List<CatalogMetadata> listCatalogs();

    List<DatabaseMetadata> listAllDatabases();

    List<TableMetadata> listAllTables();

    List<ColumnMetadata> listAllColumns();
}
