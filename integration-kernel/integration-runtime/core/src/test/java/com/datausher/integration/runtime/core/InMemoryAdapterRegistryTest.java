package com.datausher.integration.runtime.core;

import com.datausher.integration.runtime.api.AdapterCapability;
import com.datausher.integration.runtime.api.AdapterDescriptor;
import com.datausher.integration.runtime.api.AdapterHealth;
import com.datausher.integration.runtime.api.AdapterHealthStatus;
import com.datausher.integration.runtime.api.AdapterType;
import com.datausher.integration.runtime.api.IntegrationAdapter;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAdapterRegistryTest {
    @Test
    void providesStableTypedDiscoveryAndIdempotentRegistration() {
        InMemoryAdapterRegistry registry = new InMemoryAdapterRegistry();
        IntegrationAdapter mysql = adapter("mysql", "datasource.relational.query");
        IntegrationAdapter postgres = adapter("postgres", "datasource.relational.query");

        registry.register(postgres);
        assertSame(mysql, registry.register(mysql));
        assertSame(mysql, registry.register(mysql));

        assertSame(mysql, registry.find(" MYSQL ", IntegrationAdapter.class).orElseThrow());
        assertEquals(
                List.of("mysql", "postgres"),
                registry.findByType(AdapterType.DATASOURCE).stream()
                        .map(value -> value.descriptor().adapterId())
                        .toList()
        );
        assertEquals(2, registry.findByCapability("DATASOURCE.RELATIONAL.QUERY").size());
        assertSame(mysql, registry.unregister("mysql").orElseThrow());
        assertTrue(registry.find("mysql").isEmpty());
    }

    @Test
    void rejectsASecondInstanceForAnOccupiedAdapterIdentity() {
        InMemoryAdapterRegistry registry = new InMemoryAdapterRegistry();
        registry.register(adapter("mysql", "datasource.discovery"));

        assertThrows(IllegalStateException.class,
                () -> registry.register(adapter("mysql", "datasource.relational.query")));
    }

    private static IntegrationAdapter adapter(String id, String capability) {
        AdapterDescriptor descriptor = new AdapterDescriptor(
                id,
                AdapterType.DATASOURCE,
                "1.0",
                Set.of(AdapterCapability.of(capability)),
                Map.of()
        );
        return new IntegrationAdapter() {
            @Override
            public AdapterDescriptor descriptor() {
                return descriptor;
            }

            @Override
            public AdapterHealth checkHealth() {
                return new AdapterHealth(
                        id, AdapterHealthStatus.UP, Instant.EPOCH, "ready", Map.of());
            }
        };
    }
}
