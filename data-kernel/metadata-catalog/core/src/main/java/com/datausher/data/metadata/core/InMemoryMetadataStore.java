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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class InMemoryMetadataStore implements MetadataStore {
    private final Map<MetadataId, CatalogMetadata> catalogs = new HashMap<>();
    private final Map<MetadataId, DatabaseMetadata> databases = new HashMap<>();
    private final Map<MetadataId, TableMetadata> tables = new HashMap<>();
    private final Map<MetadataId, ColumnMetadata> columns = new HashMap<>();
    private final Map<MetadataId, List<TableSchema>> schemas = new HashMap<>();

    @Override
    public synchronized MetadataSyncResult synchronize(
            MetadataSyncBatch batch,
            MetadataSyncMode mode
    ) {
        MutationCounts counts = new MutationCounts();
        upsertCatalog(batch.catalog(), counts);
        batch.databases().forEach(database -> upsertDatabase(database, counts));
        batch.tables().forEach(table -> upsertTable(table, counts));
        batch.columns().forEach(column -> upsertColumn(column, counts));
        int schemaVersionsCreated = synchronizeSchemas(batch.schemas());
        if (mode == MetadataSyncMode.REPLACE) {
            counts.deleted += deleteStaleAssets(batch);
        }
        return new MetadataSyncResult(
                batch.catalog().catalogId(),
                counts.created,
                counts.updated,
                counts.unchanged,
                counts.deleted,
                schemaVersionsCreated
        );
    }

    @Override
    public synchronized Optional<CatalogMetadata> findCatalog(DatasourceId datasourceId) {
        return catalogs.values().stream()
                .filter(catalog -> catalog.datasourceId().equals(datasourceId))
                .findFirst();
    }

    @Override
    public synchronized Optional<DatabaseMetadata> findDatabase(MetadataId databaseId) {
        return Optional.ofNullable(databases.get(databaseId));
    }

    @Override
    public synchronized Optional<TableMetadata> findTable(MetadataId tableId) {
        return Optional.ofNullable(tables.get(tableId));
    }

    @Override
    public synchronized Optional<ColumnMetadata> findColumn(MetadataId columnId) {
        return Optional.ofNullable(columns.get(columnId));
    }

    @Override
    public synchronized List<DatabaseMetadata> listDatabases(MetadataId catalogId) {
        return databases.values().stream()
                .filter(database -> database.catalogId().equals(catalogId))
                .sorted(Comparator.comparing(DatabaseMetadata::qualifiedName))
                .toList();
    }

    @Override
    public synchronized List<TableMetadata> listTables(MetadataId databaseId) {
        return tables.values().stream()
                .filter(table -> table.databaseId().equals(databaseId))
                .sorted(Comparator.comparing(TableMetadata::qualifiedName))
                .toList();
    }

    @Override
    public synchronized List<ColumnMetadata> listColumns(MetadataId tableId) {
        return columns.values().stream()
                .filter(column -> column.tableId().equals(tableId))
                .sorted(Comparator.comparingInt(ColumnMetadata::ordinalPosition))
                .toList();
    }

    @Override
    public synchronized Optional<TableSchema> findCurrentSchema(MetadataId tableId) {
        List<TableSchema> versions = schemas.getOrDefault(tableId, List.of());
        return versions.isEmpty() ? Optional.empty() : Optional.of(versions.getLast());
    }

    @Override
    public synchronized List<TableSchema> listSchemaVersions(MetadataId tableId) {
        return List.copyOf(schemas.getOrDefault(tableId, List.of()));
    }

    @Override
    public synchronized List<CatalogMetadata> listCatalogs() {
        return catalogs.values().stream()
                .sorted(Comparator.comparing(CatalogMetadata::catalogId))
                .toList();
    }

    @Override
    public synchronized List<DatabaseMetadata> listAllDatabases() {
        return databases.values().stream()
                .sorted(Comparator.comparing(DatabaseMetadata::databaseId))
                .toList();
    }

    @Override
    public synchronized List<TableMetadata> listAllTables() {
        return tables.values().stream()
                .sorted(Comparator.comparing(TableMetadata::tableId))
                .toList();
    }

    @Override
    public synchronized List<ColumnMetadata> listAllColumns() {
        return columns.values().stream()
                .sorted(Comparator.comparing(ColumnMetadata::columnId))
                .toList();
    }

    private void upsertCatalog(CatalogMetadata incoming, MutationCounts counts) {
        CatalogMetadata existing = catalogs.get(incoming.catalogId());
        if (existing == null) {
            catalogs.put(incoming.catalogId(), incoming);
            counts.created++;
        } else if (sameCatalog(existing, incoming)) {
            counts.unchanged++;
        } else {
            catalogs.put(incoming.catalogId(), new CatalogMetadata(
                    incoming.catalogId(),
                    incoming.datasourceId(),
                    incoming.name(),
                    incoming.attributes(),
                    existing.createdAt(),
                    incoming.updatedAt(),
                    existing.revision() + 1
            ));
            counts.updated++;
        }
    }

    private void upsertDatabase(DatabaseMetadata incoming, MutationCounts counts) {
        DatabaseMetadata existing = databases.get(incoming.databaseId());
        if (existing == null) {
            databases.put(incoming.databaseId(), incoming);
            counts.created++;
        } else if (sameDatabase(existing, incoming)) {
            counts.unchanged++;
        } else {
            databases.put(incoming.databaseId(), new DatabaseMetadata(
                    incoming.databaseId(),
                    incoming.catalogId(),
                    incoming.name(),
                    incoming.qualifiedName(),
                    incoming.attributes(),
                    existing.createdAt(),
                    incoming.updatedAt(),
                    existing.revision() + 1
            ));
            counts.updated++;
        }
    }

    private void upsertTable(TableMetadata incoming, MutationCounts counts) {
        TableMetadata existing = tables.get(incoming.tableId());
        if (existing == null) {
            tables.put(incoming.tableId(), incoming);
            counts.created++;
        } else if (sameTable(existing, incoming)) {
            counts.unchanged++;
        } else {
            tables.put(incoming.tableId(), new TableMetadata(
                    incoming.tableId(),
                    incoming.databaseId(),
                    incoming.name(),
                    incoming.qualifiedName(),
                    incoming.kind(),
                    incoming.description(),
                    incoming.attributes(),
                    existing.createdAt(),
                    incoming.updatedAt(),
                    existing.revision() + 1
            ));
            counts.updated++;
        }
    }

    private void upsertColumn(ColumnMetadata incoming, MutationCounts counts) {
        ColumnMetadata existing = columns.get(incoming.columnId());
        if (existing == null) {
            columns.put(incoming.columnId(), incoming);
            counts.created++;
        } else if (sameColumn(existing, incoming)) {
            counts.unchanged++;
        } else {
            columns.put(incoming.columnId(), new ColumnMetadata(
                    incoming.columnId(),
                    incoming.tableId(),
                    incoming.name(),
                    incoming.qualifiedName(),
                    incoming.ordinalPosition(),
                    incoming.dataType(),
                    incoming.nullable(),
                    incoming.description(),
                    incoming.attributes(),
                    existing.createdAt(),
                    incoming.updatedAt(),
                    existing.revision() + 1
            ));
            counts.updated++;
        }
    }

    private int synchronizeSchemas(Map<MetadataId, TableSchema> incomingSchemas) {
        int created = 0;
        for (Map.Entry<MetadataId, TableSchema> entry : incomingSchemas.entrySet()) {
            List<TableSchema> versions = schemas.computeIfAbsent(
                    entry.getKey(), ignored -> new ArrayList<>());
            TableSchema incoming = entry.getValue();
            if (versions.isEmpty()) {
                versions.add(incoming);
                created++;
            } else if (!versions.getLast().fingerprint().equals(incoming.fingerprint())) {
                versions.add(new TableSchema(
                        incoming.tableId(),
                        versions.getLast().version() + 1,
                        incoming.fingerprint(),
                        incoming.columns(),
                        incoming.recordedAt()
                ));
                created++;
            }
        }
        return created;
    }

    private int deleteStaleAssets(MetadataSyncBatch batch) {
        Set<MetadataId> expectedDatabaseIds = ids(batch.databases().stream()
                .map(DatabaseMetadata::databaseId).toList());
        Set<MetadataId> expectedTableIds = ids(batch.tables().stream()
                .map(TableMetadata::tableId).toList());
        Set<MetadataId> expectedColumnIds = ids(batch.columns().stream()
                .map(ColumnMetadata::columnId).toList());

        Set<MetadataId> scopedDatabaseIds = databases.values().stream()
                .filter(database -> database.catalogId().equals(batch.catalog().catalogId()))
                .map(DatabaseMetadata::databaseId)
                .collect(java.util.stream.Collectors.toSet());
        Set<MetadataId> scopedTableIds = tables.values().stream()
                .filter(table -> scopedDatabaseIds.contains(table.databaseId()))
                .map(TableMetadata::tableId)
                .collect(java.util.stream.Collectors.toSet());

        int deleted = 0;
        for (MetadataId columnId : new HashSet<>(columns.keySet())) {
            ColumnMetadata column = columns.get(columnId);
            if (scopedTableIds.contains(column.tableId()) && !expectedColumnIds.contains(columnId)) {
                columns.remove(columnId);
                deleted++;
            }
        }
        for (MetadataId tableId : scopedTableIds) {
            if (!expectedTableIds.contains(tableId)) {
                tables.remove(tableId);
                schemas.remove(tableId);
                deleted++;
            }
        }
        for (MetadataId databaseId : scopedDatabaseIds) {
            if (!expectedDatabaseIds.contains(databaseId)) {
                databases.remove(databaseId);
                deleted++;
            }
        }
        return deleted;
    }

    private static Set<MetadataId> ids(List<MetadataId> values) {
        return Set.copyOf(values);
    }

    private static boolean sameCatalog(CatalogMetadata first, CatalogMetadata second) {
        return first.datasourceId().equals(second.datasourceId())
                && first.name().equals(second.name())
                && first.attributes().equals(second.attributes());
    }

    private static boolean sameDatabase(DatabaseMetadata first, DatabaseMetadata second) {
        return first.catalogId().equals(second.catalogId())
                && first.name().equals(second.name())
                && first.qualifiedName().equals(second.qualifiedName())
                && first.attributes().equals(second.attributes());
    }

    private static boolean sameTable(TableMetadata first, TableMetadata second) {
        return first.databaseId().equals(second.databaseId())
                && first.name().equals(second.name())
                && first.qualifiedName().equals(second.qualifiedName())
                && first.kind() == second.kind()
                && first.description().equals(second.description())
                && first.attributes().equals(second.attributes());
    }

    private static boolean sameColumn(ColumnMetadata first, ColumnMetadata second) {
        return first.tableId().equals(second.tableId())
                && first.name().equals(second.name())
                && first.qualifiedName().equals(second.qualifiedName())
                && first.ordinalPosition() == second.ordinalPosition()
                && first.dataType().equals(second.dataType())
                && first.nullable() == second.nullable()
                && first.description().equals(second.description())
                && first.attributes().equals(second.attributes());
    }

    private static final class MutationCounts {
        private int created;
        private int updated;
        private int unchanged;
        private int deleted;
    }
}
