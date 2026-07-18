package com.datausher.integration.contract;

import com.datausher.integration.datasource.api.DatasourceCapabilities;
import com.datausher.integration.datasource.api.DatasourceConnector;
import com.datausher.integration.datasource.api.RelationalDatasourceConnector;
import com.datausher.integration.datasource.api.StreamingDatasourceConnector;
import com.datausher.integration.runtime.api.AdapterType;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DatasourceConnectorContract {
    private DatasourceConnectorContract() {
    }

    public static void verify(DatasourceConnector connector, Set<String> sensitiveValues) {
        IntegrationAdapterContract.verify(connector, sensitiveValues);
        assertEquals(AdapterType.DATASOURCE, connector.descriptor().type());
        assertTrue(connector.descriptor().supports(DatasourceCapabilities.DISCOVERY),
                "datasource connectors must declare discovery capability");
        if (connector instanceof RelationalDatasourceConnector) {
            assertTrue(connector.descriptor().supports(DatasourceCapabilities.RELATIONAL_QUERY),
                    "relational connectors must declare query capability");
        }
        if (connector instanceof StreamingDatasourceConnector) {
            assertTrue(connector.descriptor().supports(DatasourceCapabilities.STREAM_READ),
                    "streaming connectors must declare stream-read capability");
        }
    }
}
