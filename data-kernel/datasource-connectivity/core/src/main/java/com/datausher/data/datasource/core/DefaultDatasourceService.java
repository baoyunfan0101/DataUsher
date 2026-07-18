package com.datausher.data.datasource.core;

import com.datausher.data.datasource.api.ChangeDatasourceStatusRequest;
import com.datausher.data.datasource.api.DatasourceCommandService;
import com.datausher.data.datasource.api.DatasourceConnectionTest;
import com.datausher.data.datasource.api.DatasourceDefinition;
import com.datausher.data.datasource.api.DatasourceDiscoveryService;
import com.datausher.data.datasource.api.DatasourceDiscoverySnapshot;
import com.datausher.data.datasource.api.DatasourceEvents;
import com.datausher.data.datasource.api.DatasourceId;
import com.datausher.data.datasource.api.DatasourceQuery;
import com.datausher.data.datasource.api.DatasourceQueryService;
import com.datausher.data.datasource.api.DatasourceStatus;
import com.datausher.data.datasource.api.DiscoverDatasourceRequest;
import com.datausher.data.datasource.api.DiscoveredDatasourceObject;
import com.datausher.data.datasource.api.DiscoveredObjectKind;
import com.datausher.data.datasource.api.RegisterDatasourceRequest;
import com.datausher.data.datasource.api.TestDatasourceConnectionRequest;
import com.datausher.integration.datasource.api.ConnectionTestResult;
import com.datausher.integration.datasource.api.DatasourceCapabilities;
import com.datausher.integration.datasource.api.DatasourceConnection;
import com.datausher.integration.datasource.api.DatasourceConnector;
import com.datausher.integration.datasource.api.DatasourceObject;
import com.datausher.integration.datasource.api.DatasourceObjectAttributes;
import com.datausher.integration.datasource.api.DiscoveryRequest;
import com.datausher.integration.runtime.api.AdapterRegistry;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.BaseDomainEvent;
import com.datausher.platform.shared.event.DomainEventPublisher;
import com.datausher.platform.shared.id.IdGenerationRequest;
import com.datausher.platform.shared.id.IdGenerator;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;
import com.datausher.platform.shared.time.Clock;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DefaultDatasourceService
        implements DatasourceCommandService, DatasourceQueryService, DatasourceDiscoveryService {
    private static final String SOURCE_MODULE = "datasource-connectivity";

    private final DatasourceStore store;
    private final AdapterRegistry adapterRegistry;
    private final Clock clock;
    private final IdGenerator idGenerator;
    private final DomainEventPublisher eventPublisher;
    private final Duration adapterTimeout;

    public DefaultDatasourceService(
            DatasourceStore store,
            AdapterRegistry adapterRegistry,
            Clock clock,
            IdGenerator idGenerator,
            DomainEventPublisher eventPublisher,
            Duration adapterTimeout
    ) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.adapterRegistry = Objects.requireNonNull(
                adapterRegistry, "adapterRegistry must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.eventPublisher = Objects.requireNonNull(
                eventPublisher, "eventPublisher must not be null");
        this.adapterTimeout = Objects.requireNonNull(
                adapterTimeout, "adapterTimeout must not be null");
        if (adapterTimeout.isZero() || adapterTimeout.isNegative()) {
            throw new IllegalArgumentException("adapterTimeout must be positive");
        }
    }

    @Override
    public DatasourceDefinition register(RegisterDatasourceRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Instant now = clock.now();
        DatasourceDefinition definition = new DatasourceDefinition(
                request.datasourceId(),
                request.displayName(),
                request.adapterId(),
                request.credentialBindingId(),
                request.connectionProperties(),
                DatasourceStatus.ACTIVE,
                now,
                now,
                1
        );
        store.create(definition);
        publish(
                DatasourceEvents.REGISTERED,
                request.requestContext(),
                now
        );
        return definition;
    }

    @Override
    public DatasourceDefinition changeStatus(ChangeDatasourceStatusRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        DatasourceDefinition current = requireDatasource(request.datasourceId());
        if (current.revision() != request.expectedRevision()) {
            throw new IllegalStateException(
                    "datasource revision does not match expectedRevision: "
                            + request.datasourceId());
        }
        if (current.status() == request.status()) {
            return current;
        }
        Instant now = clock.now();
        DatasourceDefinition updated = current.withStatus(request.status(), now);
        store.update(current, updated);
        publish(
                DatasourceEvents.STATUS_CHANGED,
                request.requestContext(),
                now
        );
        return updated;
    }

    @Override
    public Optional<DatasourceDefinition> find(DatasourceId datasourceId) {
        return store.find(Objects.requireNonNull(datasourceId, "datasourceId must not be null"));
    }

    @Override
    public PageResult<DatasourceDefinition> search(
            DatasourceQuery query,
            PageRequest pageRequest
    ) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        String searchText = query.text() == null
                ? null
                : query.text().toLowerCase(Locale.ROOT);
        List<DatasourceDefinition> matches = store.list().stream()
                .filter(definition -> query.adapterId() == null
                        || definition.adapterId().equals(query.adapterId()))
                .filter(definition -> query.status() == null
                        || definition.status() == query.status())
                .filter(definition -> searchText == null
                        || definition.datasourceId().value().contains(searchText)
                        || definition.displayName().toLowerCase(Locale.ROOT).contains(searchText))
                .sorted(Comparator.comparing(DatasourceDefinition::datasourceId))
                .toList();
        int fromIndex = (int) Math.min(pageRequest.offset(), matches.size());
        int toIndex = Math.min(fromIndex + pageRequest.size(), matches.size());
        return new PageResult<>(
                matches.subList(fromIndex, toIndex),
                matches.size(),
                pageRequest.page(),
                pageRequest.size()
        );
    }

    @Override
    public DatasourceConnectionTest testConnection(TestDatasourceConnectionRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        DatasourceDefinition definition = requireActiveDatasource(request.datasourceId());
        DatasourceConnector connector = requireConnector(definition);
        Instant testedAt = clock.now();
        ConnectionTestResult result = connector.testConnection(
                adapterContext(request.requestContext(), testedAt),
                connection(definition)
        );
        DatasourceConnectionTest test = new DatasourceConnectionTest(
                definition.datasourceId(),
                result.successful(),
                result.latency(),
                result.message(),
                result.details(),
                testedAt
        );
        publish(
                DatasourceEvents.CONNECTION_TESTED,
                request.requestContext(),
                testedAt
        );
        return test;
    }

    @Override
    public DatasourceDiscoverySnapshot discover(DiscoverDatasourceRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        DatasourceDefinition definition = requireActiveDatasource(request.datasourceId());
        DatasourceConnector connector = requireConnector(definition);
        if (!connector.descriptor().supports(DatasourceCapabilities.DISCOVERY)) {
            throw new IllegalStateException(
                    "datasource adapter does not support discovery: " + definition.adapterId());
        }
        Instant discoveredAt = clock.now();
        List<DiscoveredDatasourceObject> objects = connector.discover(
                        adapterContext(request.requestContext(), discoveredAt),
                        new DiscoveryRequest(
                                connection(definition),
                                request.namespace(),
                                request.options()
                        )
                ).stream()
                .map(DefaultDatasourceService::toDiscoveredObject)
                .sorted(Comparator
                        .comparingInt((DiscoveredDatasourceObject object) ->
                                discoveryKindOrder(object.kind()))
                        .thenComparing(object -> object.kind().value())
                        .thenComparing(DiscoveredDatasourceObject::externalId))
                .toList();
        DatasourceDiscoverySnapshot snapshot = new DatasourceDiscoverySnapshot(
                nextId("datasource-discovery"),
                definition.datasourceId(),
                request.namespace(),
                objects,
                discoveredAt
        );
        publish(
                DatasourceEvents.METADATA_DISCOVERED,
                request.requestContext(),
                discoveredAt
        );
        return snapshot;
    }

    private DatasourceDefinition requireDatasource(DatasourceId datasourceId) {
        return store.find(datasourceId).orElseThrow(() ->
                new IllegalArgumentException("datasource does not exist: " + datasourceId));
    }

    private DatasourceDefinition requireActiveDatasource(DatasourceId datasourceId) {
        DatasourceDefinition definition = requireDatasource(datasourceId);
        if (definition.status() != DatasourceStatus.ACTIVE) {
            throw new IllegalStateException("datasource is not active: " + datasourceId);
        }
        return definition;
    }

    private DatasourceConnector requireConnector(DatasourceDefinition definition) {
        return adapterRegistry.find(definition.adapterId(), DatasourceConnector.class)
                .orElseThrow(() -> new IllegalStateException(
                        "datasource adapter is not registered: " + definition.adapterId()));
    }

    private AdapterRequestContext adapterContext(RequestContext context, Instant startedAt) {
        return new AdapterRequestContext(
                context.requestId(),
                startedAt.plus(adapterTimeout),
                context.attributes()
        );
    }

    private static DatasourceConnection connection(DatasourceDefinition definition) {
        return new DatasourceConnection(
                definition.credentialBindingId(),
                definition.connectionProperties()
        );
    }

    private static DiscoveredDatasourceObject toDiscoveredObject(DatasourceObject object) {
        Map<String, String> attributes = object.attributes();
        String externalId = attributes.getOrDefault(
                DatasourceObjectAttributes.EXTERNAL_ID, object.name());
        String parentExternalId = attributes.getOrDefault(
                DatasourceObjectAttributes.PARENT_EXTERNAL_ID, "");
        return new DiscoveredDatasourceObject(
                externalId,
                parentExternalId,
                object.name(),
                DiscoveredObjectKind.fromExternalKind(object.kind()),
                attributes
        );
    }

    private static int discoveryKindOrder(DiscoveredObjectKind kind) {
        if (kind.equals(DiscoveredObjectKind.DATABASE)) {
            return 0;
        }
        if (kind.equals(DiscoveredObjectKind.TABLE)) {
            return 1;
        }
        if (kind.equals(DiscoveredObjectKind.COLUMN)) {
            return 2;
        }
        if (kind.equals(DiscoveredObjectKind.PARTITION)) {
            return 3;
        }
        return 4;
    }

    private void publish(
            String eventType,
            RequestContext requestContext,
            Instant occurredAt
    ) {
        eventPublisher.publish(new BaseDomainEvent(
                nextId("domain-event"),
                eventType,
                SOURCE_MODULE,
                occurredAt,
                requestContext
        ));
    }

    private String nextId(String entityType) {
        return idGenerator.nextIdValue(IdGenerationRequest.of("data-kernel", entityType));
    }
}
