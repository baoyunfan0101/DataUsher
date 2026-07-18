package com.datausher.data.metadata.core;

import com.datausher.data.datasource.api.DatasourceDiscoverySnapshot;
import com.datausher.data.datasource.api.DatasourceId;
import com.datausher.data.datasource.api.DiscoveredDatasourceObject;
import com.datausher.data.datasource.api.DiscoveredObjectKind;
import com.datausher.data.datasource.api.DiscoveryObjectAttributes;
import com.datausher.data.metadata.api.CatalogMetadata;
import com.datausher.data.metadata.api.ColumnMetadata;
import com.datausher.data.metadata.api.ColumnSchema;
import com.datausher.data.metadata.api.DatabaseMetadata;
import com.datausher.data.metadata.api.MetadataAssetType;
import com.datausher.data.metadata.api.MetadataCommandService;
import com.datausher.data.metadata.api.MetadataEvents;
import com.datausher.data.metadata.api.MetadataId;
import com.datausher.data.metadata.api.MetadataQueryService;
import com.datausher.data.metadata.api.MetadataSearchHit;
import com.datausher.data.metadata.api.MetadataSearchQuery;
import com.datausher.data.metadata.api.MetadataSearchService;
import com.datausher.data.metadata.api.MetadataSyncResult;
import com.datausher.data.metadata.api.SchemaQueryService;
import com.datausher.data.metadata.api.SynchronizeMetadataRequest;
import com.datausher.data.metadata.api.TableKind;
import com.datausher.data.metadata.api.TableMetadata;
import com.datausher.data.metadata.api.TableSchema;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.BaseDomainEvent;
import com.datausher.platform.shared.event.DomainEventPublisher;
import com.datausher.platform.shared.id.IdGenerationRequest;
import com.datausher.platform.shared.id.IdGenerator;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;
import com.datausher.platform.shared.time.Clock;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class DefaultMetadataCatalogService implements
        MetadataCommandService,
        MetadataQueryService,
        MetadataSearchService,
        SchemaQueryService {
    private static final String SOURCE_MODULE = "metadata-catalog";

    private final MetadataStore store;
    private final MetadataIdFactory idFactory;
    private final Clock clock;
    private final IdGenerator idGenerator;
    private final DomainEventPublisher eventPublisher;

    public DefaultMetadataCatalogService(
            MetadataStore store,
            MetadataIdFactory idFactory,
            Clock clock,
            IdGenerator idGenerator,
            DomainEventPublisher eventPublisher
    ) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.idFactory = Objects.requireNonNull(idFactory, "idFactory must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.eventPublisher = Objects.requireNonNull(
                eventPublisher, "eventPublisher must not be null");
    }

    @Override
    public MetadataSyncResult synchronize(SynchronizeMetadataRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Instant synchronizedAt = clock.now();
        MetadataSyncBatch batch = buildBatch(request, synchronizedAt);
        MetadataSyncResult result = store.synchronize(batch, request.mode());
        publish(
                MetadataEvents.TABLE_METADATA_SYNCED,
                request.requestContext(),
                synchronizedAt
        );
        if (result.schemaVersionsCreated() > 0) {
            publish(
                    MetadataEvents.TABLE_SCHEMA_CHANGED,
                    request.requestContext(),
                    synchronizedAt
            );
        }
        return result;
    }

    @Override
    public Optional<CatalogMetadata> findCatalog(DatasourceId datasourceId) {
        return store.findCatalog(Objects.requireNonNull(
                datasourceId, "datasourceId must not be null"));
    }

    @Override
    public Optional<DatabaseMetadata> findDatabase(MetadataId databaseId) {
        return store.findDatabase(Objects.requireNonNull(databaseId, "databaseId must not be null"));
    }

    @Override
    public Optional<TableMetadata> findTable(MetadataId tableId) {
        return store.findTable(Objects.requireNonNull(tableId, "tableId must not be null"));
    }

    @Override
    public Optional<ColumnMetadata> findColumn(MetadataId columnId) {
        return store.findColumn(Objects.requireNonNull(columnId, "columnId must not be null"));
    }

    @Override
    public List<DatabaseMetadata> listDatabases(MetadataId catalogId) {
        return store.listDatabases(Objects.requireNonNull(catalogId, "catalogId must not be null"));
    }

    @Override
    public List<TableMetadata> listTables(MetadataId databaseId) {
        return store.listTables(Objects.requireNonNull(databaseId, "databaseId must not be null"));
    }

    @Override
    public List<ColumnMetadata> listColumns(MetadataId tableId) {
        return store.listColumns(Objects.requireNonNull(tableId, "tableId must not be null"));
    }

    @Override
    public Optional<TableSchema> findCurrentSchema(MetadataId tableId) {
        return store.findCurrentSchema(Objects.requireNonNull(tableId, "tableId must not be null"));
    }

    @Override
    public List<TableSchema> listSchemaVersions(MetadataId tableId) {
        return store.listSchemaVersions(Objects.requireNonNull(tableId, "tableId must not be null"));
    }

    @Override
    public PageResult<MetadataSearchHit> search(
            MetadataSearchQuery query,
            PageRequest pageRequest
    ) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        return store.search(query, pageRequest);
    }

    private MetadataSyncBatch buildBatch(
            SynchronizeMetadataRequest request,
            Instant synchronizedAt
    ) {
        DatasourceDiscoverySnapshot discovery = request.discovery();
        DatasourceId datasourceId = discovery.datasourceId();
        MetadataId catalogId = idFactory.create(
                MetadataAssetType.CATALOG, datasourceId, datasourceId.value());
        CatalogMetadata catalog = new CatalogMetadata(
                catalogId,
                datasourceId,
                request.catalogName(),
                Map.of(),
                synchronizedAt,
                synchronizedAt,
                1
        );

        Map<String, DatabaseMetadata> databasesByExternalId = new HashMap<>();
        for (DiscoveredDatasourceObject object : objects(discovery, DiscoveredObjectKind.DATABASE)) {
            MetadataId databaseId = idFactory.create(
                    MetadataAssetType.DATABASE, datasourceId, object.externalId());
            DatabaseMetadata database = new DatabaseMetadata(
                    databaseId,
                    catalogId,
                    object.name(),
                    qualifiedName(object),
                    object.attributes(),
                    synchronizedAt,
                    synchronizedAt,
                    1
            );
            databasesByExternalId.put(object.externalId(), database);
        }

        Map<String, TableMetadata> tablesByExternalId = new HashMap<>();
        for (DiscoveredDatasourceObject object : objects(discovery, DiscoveredObjectKind.TABLE)) {
            DatabaseMetadata database = requireParent(
                    databasesByExternalId, object, "database");
            MetadataId tableId = idFactory.create(
                    MetadataAssetType.TABLE, datasourceId, object.externalId());
            TableMetadata table = new TableMetadata(
                    tableId,
                    database.databaseId(),
                    object.name(),
                    qualifiedName(object),
                    TableKind.fromExternalValue(object.attributes().get(
                            DiscoveryObjectAttributes.TABLE_TYPE)),
                    object.attributes().get(DiscoveryObjectAttributes.REMARKS),
                    object.attributes(),
                    synchronizedAt,
                    synchronizedAt,
                    1
            );
            tablesByExternalId.put(object.externalId(), table);
        }

        List<ColumnMetadata> columns = new ArrayList<>();
        for (DiscoveredDatasourceObject object : objects(discovery, DiscoveredObjectKind.COLUMN)) {
            TableMetadata table = requireParent(tablesByExternalId, object, "table");
            columns.add(new ColumnMetadata(
                    idFactory.create(MetadataAssetType.COLUMN, datasourceId, object.externalId()),
                    table.tableId(),
                    object.name(),
                    qualifiedName(object),
                    positiveInteger(object, DiscoveryObjectAttributes.ORDINAL_POSITION),
                    requiredAttribute(object, DiscoveryObjectAttributes.DATA_TYPE),
                    booleanAttribute(object, DiscoveryObjectAttributes.NULLABLE),
                    object.attributes().get(DiscoveryObjectAttributes.REMARKS),
                    object.attributes(),
                    synchronizedAt,
                    synchronizedAt,
                    1
            ));
        }

        Map<MetadataId, List<ColumnMetadata>> columnsByTable = columns.stream()
                .collect(Collectors.groupingBy(ColumnMetadata::tableId));
        Map<MetadataId, TableSchema> schemas = new HashMap<>();
        for (TableMetadata table : tablesByExternalId.values()) {
            List<ColumnSchema> columnSchemas = columnsByTable
                    .getOrDefault(table.tableId(), List.of()).stream()
                    .sorted(Comparator.comparingInt(ColumnMetadata::ordinalPosition))
                    .map(column -> new ColumnSchema(
                            column.name(),
                            column.ordinalPosition(),
                            column.dataType(),
                            column.nullable()
                    ))
                    .toList();
            schemas.put(table.tableId(), new TableSchema(
                    table.tableId(),
                    1,
                    schemaFingerprint(columnSchemas),
                    columnSchemas,
                    synchronizedAt
            ));
        }

        return new MetadataSyncBatch(
                catalog,
                List.copyOf(databasesByExternalId.values()),
                List.copyOf(tablesByExternalId.values()),
                columns,
                schemas
        );
    }

    private static List<DiscoveredDatasourceObject> objects(
            DatasourceDiscoverySnapshot discovery,
            DiscoveredObjectKind kind
    ) {
        return discovery.objects().stream().filter(object -> object.kind().equals(kind)).toList();
    }

    private static <T> T requireParent(
            Map<String, T> parents,
            DiscoveredDatasourceObject object,
            String parentType
    ) {
        T parent = parents.get(object.parentExternalId());
        if (parent == null) {
            throw new IllegalArgumentException(
                    "discovered " + object.kind().value()
                            + " references an unknown " + parentType + ": "
                            + object.parentExternalId());
        }
        return parent;
    }

    private static String requiredAttribute(
            DiscoveredDatasourceObject object,
            String attribute
    ) {
        String value = object.attributes().get(attribute);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "discovered object is missing " + attribute + ": " + object.externalId());
        }
        return value.trim();
    }

    private static String qualifiedName(DiscoveredDatasourceObject object) {
        return object.attributes().getOrDefault(
                DiscoveryObjectAttributes.QUALIFIED_NAME, object.externalId());
    }

    private static int positiveInteger(
            DiscoveredDatasourceObject object,
            String attribute
    ) {
        try {
            int value = Integer.parseInt(requiredAttribute(object, attribute));
            if (value < 1) {
                throw new NumberFormatException("not positive");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    attribute + " must be a positive integer: " + object.externalId(), exception);
        }
    }

    private static boolean booleanAttribute(
            DiscoveredDatasourceObject object,
            String attribute
    ) {
        String value = requiredAttribute(object, attribute).toLowerCase(Locale.ROOT);
        if (!value.equals("true") && !value.equals("false")) {
            throw new IllegalArgumentException(
                    attribute + " must be true or false: " + object.externalId());
        }
        return Boolean.parseBoolean(value);
    }

    private static String schemaFingerprint(List<ColumnSchema> columns) {
        StringBuilder canonical = new StringBuilder();
        columns.forEach(column -> canonical
                .append(column.ordinalPosition()).append('\u0000')
                .append(column.name()).append('\u0000')
                .append(column.dataType()).append('\u0000')
                .append(column.nullable()).append('\u0001'));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private void publish(
            String eventType,
            RequestContext requestContext,
            Instant occurredAt
    ) {
        eventPublisher.publish(new BaseDomainEvent(
                idGenerator.nextIdValue(IdGenerationRequest.of("data-kernel", "domain-event")),
                eventType,
                SOURCE_MODULE,
                occurredAt,
                requestContext
        ));
    }
}
