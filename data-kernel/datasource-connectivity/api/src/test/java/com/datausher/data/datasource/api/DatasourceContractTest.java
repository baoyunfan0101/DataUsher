package com.datausher.data.datasource.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatasourceContractTest {
    private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");

    @Test
    void rejectsCredentialsInConnectionProperties() {
        assertThrows(IllegalArgumentException.class, () -> definition(
                Map.of("host", "localhost", "password", "unsafe")));
        assertThrows(IllegalArgumentException.class, () -> definition(
                Map.of("api_token", "unsafe")));
    }

    @Test
    void normalizesIdentityAndProtectsConnectionProperties() {
        Map<String, String> properties = new HashMap<>(Map.of("SSL.MODE", "required"));

        DatasourceDefinition definition = definition(properties);
        properties.put("SSL.MODE", "disabled");

        assertEquals(new DatasourceId("analytics"), definition.datasourceId());
        assertEquals("mysql", definition.adapterId());
        assertEquals("required", definition.connectionProperties().get("ssl.mode"));
    }

    @Test
    void preservesFutureDiscoveryKindsWithoutChangingTheContract() {
        DiscoveredObjectKind topic = DiscoveredObjectKind.fromExternalKind("topic");

        assertEquals(new DiscoveredObjectKind("topic"), topic);
        assertEquals("topic", topic.value());
    }

    private static DatasourceDefinition definition(Map<String, String> properties) {
        return new DatasourceDefinition(
                new DatasourceId(" Analytics "),
                "Analytics",
                "MySQL",
                "Analytics-Credential",
                properties,
                DatasourceStatus.ACTIVE,
                NOW,
                NOW,
                1
        );
    }
}
