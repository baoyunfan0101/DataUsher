package com.datausher.data.datasource.core;

import com.datausher.data.datasource.api.ChangeDatasourceStatusRequest;
import com.datausher.data.datasource.api.DatasourceDefinition;
import com.datausher.data.datasource.api.DatasourceDiscoverySnapshot;
import com.datausher.data.datasource.api.DatasourceEvents;
import com.datausher.data.datasource.api.DatasourceId;
import com.datausher.data.datasource.api.DatasourceStatus;
import com.datausher.data.datasource.api.DiscoverDatasourceRequest;
import com.datausher.data.datasource.api.RegisterDatasourceRequest;
import com.datausher.data.datasource.api.TestDatasourceConnectionRequest;
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
import com.datausher.platform.shared.event.DomainEvent;
import com.datausher.platform.shared.id.GeneratedId;
import com.datausher.platform.shared.id.IdFormat;
import com.datausher.platform.shared.id.IdGenerator;
import com.datausher.platform.shared.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultDatasourceServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");
    private static final DatasourceId DATASOURCE_ID = new DatasourceId("analytics");

    private final InMemoryDatasourceStore store = new InMemoryDatasourceStore();
    private final InMemoryAdapterRegistry adapterRegistry = new InMemoryAdapterRegistry();
    private final List<DomainEvent> events = new ArrayList<>();
    private DefaultDatasourceService service;

    @BeforeEach
    void setUp() {
        adapterRegistry.register(new StubDatasourceConnector());
        AtomicLong sequence = new AtomicLong();
        IdGenerator idGenerator = request -> new GeneratedId(
                "test-" + sequence.incrementAndGet(),
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
        service = new DefaultDatasourceService(
                store,
                adapterRegistry,
                clock,
                idGenerator,
                events::add,
                Duration.ofSeconds(30)
        );
    }

    @Test
    void registersTestsAndDiscoversThroughTheConnectorBoundary() {
        DatasourceDefinition registered = service.register(registration());

        assertTrue(service.testConnection(new TestDatasourceConnectionRequest(
                DATASOURCE_ID, context())).successful());
        DatasourceDiscoverySnapshot snapshot = service.discover(new DiscoverDatasourceRequest(
                DATASOURCE_ID, "", Map.of(), context()));

        assertEquals(1, registered.revision());
        assertEquals(3, snapshot.objects().size());
        assertEquals("analytics.public.orders.id", snapshot.objects().get(2).externalId());
        assertEquals(
                List.of(
                        DatasourceEvents.REGISTERED,
                        DatasourceEvents.CONNECTION_TESTED,
                        DatasourceEvents.METADATA_DISCOVERED
                ),
                events.stream().map(DomainEvent::eventType).toList()
        );
    }

    @Test
    void enforcesOptimisticRevisionAndDisabledDatasourceState() {
        service.register(registration());

        assertThrows(IllegalStateException.class, () -> service.changeStatus(
                new ChangeDatasourceStatusRequest(
                        DATASOURCE_ID, DatasourceStatus.DISABLED, 2, context())));
        DatasourceDefinition disabled = service.changeStatus(
                new ChangeDatasourceStatusRequest(
                        DATASOURCE_ID, DatasourceStatus.DISABLED, 1, context()));

        assertEquals(2, disabled.revision());
        assertThrows(IllegalStateException.class, () -> service.discover(
                new DiscoverDatasourceRequest(DATASOURCE_ID, "", Map.of(), context())));
    }

    private static RegisterDatasourceRequest registration() {
        return new RegisterDatasourceRequest(
                DATASOURCE_ID,
                "Analytics",
                "mysql",
                "analytics-credential",
                Map.of("ssl.mode", "required"),
                context()
        );
    }

    private static RequestContext context() {
        return RequestContext.system("request-1", NOW);
    }

    private static final class StubDatasourceConnector implements DatasourceConnector {
        private static final AdapterDescriptor DESCRIPTOR = new AdapterDescriptor(
                "mysql",
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
            return new ConnectionTestResult(true, Duration.ofMillis(2), "ok", Map.of());
        }

        @Override
        public List<DatasourceObject> discover(
                AdapterRequestContext context,
                DiscoveryRequest request
        ) {
            return List.of(
                    object("analytics", "", "analytics", DatasourceObjectKinds.DATABASE),
                    object("analytics.public.orders", "analytics", "orders",
                            DatasourceObjectKinds.TABLE),
                    object("analytics.public.orders.id", "analytics.public.orders", "id",
                            DatasourceObjectKinds.COLUMN)
            );
        }

        @Override
        public AdapterDescriptor descriptor() {
            return DESCRIPTOR;
        }

        @Override
        public AdapterHealth checkHealth() {
            return new AdapterHealth(
                    "mysql", AdapterHealthStatus.UP, NOW, "ok", Map.of());
        }

        private static DatasourceObject object(
                String externalId,
                String parentExternalId,
                String name,
                String kind
        ) {
            Map<String, String> attributes = parentExternalId.isEmpty()
                    ? Map.of(DatasourceObjectAttributes.EXTERNAL_ID, externalId)
                    : Map.of(
                            DatasourceObjectAttributes.EXTERNAL_ID, externalId,
                            DatasourceObjectAttributes.PARENT_EXTERNAL_ID, parentExternalId
                    );
            return new DatasourceObject(name, kind, attributes);
        }
    }
}
