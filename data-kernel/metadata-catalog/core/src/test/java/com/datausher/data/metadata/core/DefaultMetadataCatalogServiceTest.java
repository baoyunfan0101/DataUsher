package com.datausher.data.metadata.core;

import com.datausher.data.datasource.api.DatasourceDiscoverySnapshot;
import com.datausher.data.datasource.api.DatasourceId;
import com.datausher.data.datasource.api.DiscoveredDatasourceObject;
import com.datausher.data.datasource.api.DiscoveredObjectKind;
import com.datausher.data.datasource.api.DiscoveryObjectAttributes;
import com.datausher.data.metadata.api.CatalogMetadata;
import com.datausher.data.metadata.api.DatabaseMetadata;
import com.datausher.data.metadata.api.MetadataEvents;
import com.datausher.data.metadata.api.MetadataSearchQuery;
import com.datausher.data.metadata.api.MetadataSyncMode;
import com.datausher.data.metadata.api.MetadataSyncResult;
import com.datausher.data.metadata.api.SynchronizeMetadataRequest;
import com.datausher.data.metadata.api.TableMetadata;
import com.datausher.data.metadata.api.TableSchema;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;
import com.datausher.platform.shared.id.GeneratedId;
import com.datausher.platform.shared.id.IdFormat;
import com.datausher.platform.shared.id.IdGenerator;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultMetadataCatalogServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");
    private static final DatasourceId DATASOURCE_ID = new DatasourceId("analytics");

    private final InMemoryMetadataStore store = new InMemoryMetadataStore();
    private final List<DomainEvent> events = new ArrayList<>();
    private DefaultMetadataCatalogService service;

    @BeforeEach
    void setUp() {
        AtomicLong sequence = new AtomicLong();
        IdGenerator idGenerator = request -> new GeneratedId(
                "event-" + sequence.incrementAndGet(),
                IdFormat.CUSTOM,
                request,
                Map.of()
        );
        Clock clock = new Clock() {
            @Override
            public Instant now() {
                return NOW;
            }

            @Override
            public ZoneId zone() {
                return ZoneOffset.UTC;
            }
        };
        service = new DefaultMetadataCatalogService(
                store,
                new Sha256MetadataIdFactory(),
                clock,
                idGenerator,
                events::add
        );
    }

    @Test
    void synchronizesSearchesAndVersionsMetadata() {
        MetadataSyncResult first = service.synchronize(request(snapshot("bigint", true)));
        MetadataSyncResult repeated = service.synchronize(request(snapshot("bigint", true)));
        MetadataSyncResult changed = service.synchronize(request(snapshot("varchar", true)));

        CatalogMetadata catalog = service.findCatalog(DATASOURCE_ID).orElseThrow();
        DatabaseMetadata database = service.listDatabases(catalog.catalogId()).getFirst();
        TableMetadata table = service.listTables(database.databaseId()).getFirst();
        List<TableSchema> schemas = service.listSchemaVersions(table.tableId());

        assertEquals(5, first.created());
        assertEquals(1, first.schemaVersionsCreated());
        assertEquals(5, repeated.unchanged());
        assertEquals(0, repeated.schemaVersionsCreated());
        assertEquals(1, changed.updated());
        assertEquals(1, changed.schemaVersionsCreated());
        assertEquals(2, schemas.size());
        assertEquals("varchar", schemas.getLast().columns().getFirst().dataType());
        assertEquals("orders", service.search(
                new MetadataSearchQuery("orders", DATASOURCE_ID, Set.of()),
                PageRequest.firstPage()
        ).items().getFirst().name());
        assertEquals(
                List.of(
                        MetadataEvents.TABLE_METADATA_SYNCED,
                        MetadataEvents.TABLE_SCHEMA_CHANGED,
                        MetadataEvents.TABLE_METADATA_SYNCED,
                        MetadataEvents.TABLE_METADATA_SYNCED,
                        MetadataEvents.TABLE_SCHEMA_CHANGED
                ),
                events.stream().map(DomainEvent::eventType).toList()
        );
    }

    @Test
    void rejectsDiscoveryObjectsWithBrokenHierarchy() {
        DatasourceDiscoverySnapshot invalid = new DatasourceDiscoverySnapshot(
                "discovery-invalid",
                DATASOURCE_ID,
                "",
                List.of(new DiscoveredDatasourceObject(
                        "analytics.missing.orders",
                        "analytics.missing",
                        "orders",
                        DiscoveredObjectKind.TABLE,
                        Map.of()
                )),
                NOW
        );

        assertThrows(IllegalArgumentException.class, () -> service.synchronize(request(invalid)));
    }

    private static SynchronizeMetadataRequest request(DatasourceDiscoverySnapshot snapshot) {
        return new SynchronizeMetadataRequest(
                snapshot,
                "Analytics",
                MetadataSyncMode.UPSERT,
                RequestContext.system("request-1", NOW)
        );
    }

    private static DatasourceDiscoverySnapshot snapshot(String idType, boolean includeName) {
        List<DiscoveredDatasourceObject> objects = new ArrayList<>();
        objects.add(object(
                "analytics",
                "",
                "analytics",
                DiscoveredObjectKind.DATABASE,
                Map.of()
        ));
        objects.add(object(
                "analytics.orders",
                "analytics",
                "orders",
                DiscoveredObjectKind.TABLE,
                Map.of(DiscoveryObjectAttributes.TABLE_TYPE, "TABLE")
        ));
        objects.add(column(
                "analytics.orders.id", "analytics.orders", "id", 1, idType, false));
        if (includeName) {
            objects.add(column(
                    "analytics.orders.name", "analytics.orders", "name", 2,
                    "varchar", true));
        }
        return new DatasourceDiscoverySnapshot(
                "discovery-1",
                DATASOURCE_ID,
                "",
                objects,
                NOW
        );
    }

    private static DiscoveredDatasourceObject column(
            String externalId,
            String parentExternalId,
            String name,
            int position,
            String dataType,
            boolean nullable
    ) {
        return object(
                externalId,
                parentExternalId,
                name,
                DiscoveredObjectKind.COLUMN,
                Map.of(
                        DiscoveryObjectAttributes.ORDINAL_POSITION, Integer.toString(position),
                        DiscoveryObjectAttributes.DATA_TYPE, dataType,
                        DiscoveryObjectAttributes.NULLABLE, Boolean.toString(nullable)
                )
        );
    }

    private static DiscoveredDatasourceObject object(
            String externalId,
            String parentExternalId,
            String name,
            DiscoveredObjectKind kind,
            Map<String, String> attributes
    ) {
        return new DiscoveredDatasourceObject(
                externalId,
                parentExternalId,
                name,
                kind,
                attributes
        );
    }
}
