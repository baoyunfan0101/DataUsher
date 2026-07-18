package com.datausher.integration.datasource.api;

import com.datausher.integration.runtime.api.IntegrationValue;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatasourceContractTest {
    @Test
    void normalizesBindingIdentityAndCopiesOptions() {
        Map<String, String> options = new java.util.HashMap<>(Map.of("ssl", "true"));
        DatasourceConnection connection = new DatasourceConnection(" Analytics ", options);
        options.put("ssl", "false");

        assertEquals("analytics", connection.bindingId());
        assertEquals("true", connection.options().get("ssl"));
    }

    @Test
    void rejectsRowsThatDoNotMatchTheDeclaredSchema() {
        assertThrows(IllegalArgumentException.class,
                () -> new QueryResult(
                        List.of("id", "name"),
                        List.of(List.of(new IntegrationValue.DecimalValue(1))),
                        false
                ));
    }

    @Test
    void publishesCanonicalCapabilitiesForPortableDiscovery() {
        assertTrue(DatasourceCapabilities.DISCOVERY.startsWith("datasource."));
        assertTrue(DatasourceCapabilities.RELATIONAL_QUERY.startsWith("datasource."));
        assertTrue(DatasourceCapabilities.STREAM_READ.startsWith("datasource."));
    }

    @Test
    void requiresAUsefulMessageForAnUnsuccessfulConnectionTest() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConnectionTestResult(false, Duration.ZERO, "", Map.of()));
    }
}
