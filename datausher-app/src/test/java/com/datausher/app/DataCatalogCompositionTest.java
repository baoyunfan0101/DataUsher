package com.datausher.app;

import com.datausher.data.datasource.api.DatasourceId;
import com.datausher.data.datasource.api.DiscoverDatasourceRequest;
import com.datausher.data.datasource.api.RegisterDatasourceRequest;
import com.datausher.data.datasource.core.DefaultDatasourceService;
import com.datausher.data.datasource.core.InMemoryDatasourceStore;
import com.datausher.data.metadata.api.MetadataAssetType;
import com.datausher.data.metadata.api.MetadataSearchQuery;
import com.datausher.data.metadata.api.MetadataSyncMode;
import com.datausher.data.metadata.api.SynchronizeMetadataRequest;
import com.datausher.data.metadata.core.DefaultMetadataCatalogService;
import com.datausher.data.metadata.core.InMemoryMetadataStore;
import com.datausher.data.metadata.core.Sha256MetadataIdFactory;
import com.datausher.integration.datasource.api.ConnectionTestResult;
import com.datausher.integration.datasource.api.DatasourceCapabilities;
import com.datausher.integration.datasource.api.DatasourceConnection;
import com.datausher.integration.datasource.api.DatasourceConnector;
import com.datausher.integration.datasource.api.DatasourceObject;
import com.datausher.integration.datasource.api.DatasourceObjectAttributes;
import com.datausher.integration.datasource.api.DatasourceObjectKinds;
import com.datausher.integration.datasource.api.DiscoveryRequest;
import com.datausher.integration.runtime.api.AdapterCapability;
import com.datausher.integration.runtime.api.AdapterDescriptor;
import com.datausher.integration.runtime.api.AdapterHealth;
import com.datausher.integration.runtime.api.AdapterHealthStatus;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.AdapterType;
import com.datausher.integration.runtime.core.InMemoryAdapterRegistry;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.id.core.UuidIdGenerator;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.time.core.SystemClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataCatalogCompositionTest {
    @Test
    void discoversAndCatalogsDatasourceMetadataAcrossModuleBoundaries() {
        var clock = new SystemClock();
        var ids = new UuidIdGenerator();
        var adapters = new InMemoryAdapterRegistry();
        adapters.register(new CatalogFixtureConnector());
        var datasources = new DefaultDatasourceService(
                new InMemoryDatasourceStore(),
                adapters,
                clock,
                ids,
                event -> { },
                Duration.ofSeconds(30)
        );
        var catalog = new DefaultMetadataCatalogService(
                new InMemoryMetadataStore(),
                new Sha256MetadataIdFactory(),
                clock,
                ids,
                event -> { }
        );
        var context = RequestContext.system("request-1", clock.now());
        var datasourceId = new DatasourceId("analytics");

        datasources.register(new RegisterDatasourceRequest(
                datasourceId,
                "Analytics",
                "catalog-fixture",
                "analytics-credential",
                Map.of(),
                context
        ));
        var discovery = datasources.discover(new DiscoverDatasourceRequest(
                datasourceId, "", Map.of(), context));
        var result = catalog.synchronize(new SynchronizeMetadataRequest(
                discovery, "Analytics", MetadataSyncMode.REPLACE, context));
        var hits = catalog.search(
                new MetadataSearchQuery("orders", datasourceId, Set.of(MetadataAssetType.TABLE)),
                PageRequest.firstPage()
        );

        assertEquals(4, result.created());
        assertEquals(1, result.schemaVersionsCreated());
        assertEquals(1, hits.total());
        assertEquals("analytics.orders", hits.items().getFirst().qualifiedName());
    }

    private static final class CatalogFixtureConnector implements DatasourceConnector {
        private static final AdapterDescriptor DESCRIPTOR = new AdapterDescriptor(
                "catalog-fixture",
                AdapterType.DATASOURCE,
                "1.0.0",
                Set.of(AdapterCapability.of(DatasourceCapabilities.DISCOVERY)),
                Map.of()
        );

        @Override
        public ConnectionTestResult testConnection(
                AdapterRequestContext context,
                DatasourceConnection connection
        ) {
            return new ConnectionTestResult(true, Duration.ZERO, "connected", Map.of());
        }

        @Override
        public List<DatasourceObject> discover(
                AdapterRequestContext context,
                DiscoveryRequest request
        ) {
            return List.of(
                    object("analytics", "", "analytics", DatasourceObjectKinds.DATABASE,
                            Map.of(DatasourceObjectAttributes.QUALIFIED_NAME, "analytics")),
                    object("analytics.orders", "analytics", "orders", DatasourceObjectKinds.TABLE,
                            Map.of(
                                    DatasourceObjectAttributes.QUALIFIED_NAME, "analytics.orders",
                                    DatasourceObjectAttributes.TABLE_TYPE, "TABLE"
                            )),
                    object("analytics.orders.id", "analytics.orders", "id",
                            DatasourceObjectKinds.COLUMN,
                            Map.of(
                                    DatasourceObjectAttributes.QUALIFIED_NAME, "analytics.orders.id",
                                    DatasourceObjectAttributes.ORDINAL_POSITION, "1",
                                    DatasourceObjectAttributes.DATA_TYPE, "BIGINT",
                                    DatasourceObjectAttributes.NULLABLE, "false"
                            ))
            );
        }

        @Override
        public AdapterDescriptor descriptor() {
            return DESCRIPTOR;
        }

        @Override
        public AdapterHealth checkHealth() {
            return new AdapterHealth(
                    DESCRIPTOR.adapterId(),
                    AdapterHealthStatus.UP,
                    java.time.Instant.EPOCH,
                    "ready",
                    Map.of()
            );
        }

        private static DatasourceObject object(
                String externalId,
                String parentExternalId,
                String name,
                String kind,
                Map<String, String> extraAttributes
        ) {
            var attributes = new java.util.LinkedHashMap<>(extraAttributes);
            attributes.put(DatasourceObjectAttributes.EXTERNAL_ID, externalId);
            if (!parentExternalId.isEmpty()) {
                attributes.put(DatasourceObjectAttributes.PARENT_EXTERNAL_ID, parentExternalId);
            }
            return new DatasourceObject(name, kind, attributes);
        }
    }
}
