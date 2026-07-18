package com.datausher.app;

import com.datausher.integration.contract.DatasourceConnectorContract;
import com.datausher.integration.datasource.api.ConnectionTestResult;
import com.datausher.integration.datasource.api.DatasourceConnection;
import com.datausher.integration.datasource.api.DatasourceCapabilities;
import com.datausher.integration.datasource.api.DatasourceObject;
import com.datausher.integration.datasource.api.DiscoveryRequest;
import com.datausher.integration.datasource.api.QueryRequest;
import com.datausher.integration.datasource.api.QueryResult;
import com.datausher.integration.datasource.api.RelationalDatasourceConnector;
import com.datausher.integration.runtime.api.AdapterCapability;
import com.datausher.integration.runtime.api.AdapterDescriptor;
import com.datausher.integration.runtime.api.AdapterHealth;
import com.datausher.integration.runtime.api.AdapterHealthStatus;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.AdapterType;
import com.datausher.integration.runtime.api.ExternalSystemException;
import com.datausher.integration.runtime.api.IntegrationErrorCode;
import com.datausher.integration.runtime.api.IntegrationValue;
import com.datausher.integration.runtime.core.DefaultIntegrationErrorMapper;
import com.datausher.integration.runtime.core.InMemoryAdapterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatasourceAdapterCompositionTest {
    @Test
    void resolvesReplaceableDatasourceContractAndMapsVendorFailure() {
        var registry = new InMemoryAdapterRegistry();
        registry.register(new ContractTestDatasourceConnector());

        RelationalDatasourceConnector connector = registry
                .find("contract-test-datasource", RelationalDatasourceConnector.class)
                .orElseThrow();
        DatasourceConnectorContract.verify(connector, Set.of());
        AdapterRequestContext context = new AdapterRequestContext(
                "request-1", Instant.MAX, Map.of());
        QueryResult result = connector.executeQuery(
                context,
                new QueryRequest(
                        new DatasourceConnection("analytics", Map.of()),
                        "select 1",
                        List.of(),
                        10
                )
        );
        var error = new DefaultIntegrationErrorMapper().map(
                connector.descriptor().adapterId(),
                new ExternalSystemException(
                        IntegrationErrorCode.UNAVAILABLE,
                        "vendor endpoint is unavailable",
                        true,
                        Map.of("vendor", "contract-test"),
                        null
                )
        );

        assertEquals(
                List.of(List.of(new IntegrationValue.DecimalValue(1))),
                result.rows()
        );
        assertEquals(IntegrationErrorCode.UNAVAILABLE, error.code());
        assertTrue(error.retryable());
    }

    private static final class ContractTestDatasourceConnector
            implements RelationalDatasourceConnector {
        private final AdapterDescriptor descriptor = new AdapterDescriptor(
                "contract-test-datasource",
                AdapterType.DATASOURCE,
                "1.0",
                Set.of(
                        AdapterCapability.of(DatasourceCapabilities.DISCOVERY),
                        AdapterCapability.of(DatasourceCapabilities.RELATIONAL_QUERY)
                ),
                Map.of()
        );

        @Override
        public AdapterDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public AdapterHealth checkHealth() {
            return new AdapterHealth(
                    descriptor.adapterId(),
                    AdapterHealthStatus.UP,
                    Instant.EPOCH,
                    "ready",
                    Map.of()
            );
        }

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
            return List.of(new DatasourceObject("sample", "table", Map.of()));
        }

        @Override
        public QueryResult executeQuery(AdapterRequestContext context, QueryRequest request) {
            return new QueryResult(
                    List.of("value"),
                    List.of(List.of(new IntegrationValue.DecimalValue(1))),
                    false
            );
        }
    }
}
