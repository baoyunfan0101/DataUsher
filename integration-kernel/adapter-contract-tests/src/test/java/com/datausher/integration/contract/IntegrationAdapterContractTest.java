package com.datausher.integration.contract;

import com.datausher.integration.runtime.api.AdapterCapability;
import com.datausher.integration.runtime.api.AdapterDescriptor;
import com.datausher.integration.runtime.api.AdapterHealth;
import com.datausher.integration.runtime.api.AdapterHealthStatus;
import com.datausher.integration.runtime.api.AdapterType;
import com.datausher.integration.runtime.api.IntegrationAdapter;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;

class IntegrationAdapterContractTest {
    @Test
    void acceptsStableDescriptorsAndSafeHealthDetails() {
        IntegrationAdapterContract.verify(adapter("healthy", Map.of("region", "local")),
                Set.of("secret-value"));
    }

    @Test
    void rejectsSensitiveHealthDetails() {
        IntegrationAdapter adapter = adapter("healthy", Map.of("error", "secret-value"));

        assertThrows(AssertionError.class,
                () -> IntegrationAdapterContract.verify(adapter, Set.of("secret-value")));
    }

    private static IntegrationAdapter adapter(String message, Map<String, String> details) {
        AdapterDescriptor descriptor = new AdapterDescriptor(
                "adapter-1",
                AdapterType.DATASOURCE,
                "1.0.0",
                Set.of(AdapterCapability.of("datasource.discovery")),
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
                        descriptor.adapterId(),
                        AdapterHealthStatus.UP,
                        Instant.parse("2026-07-18T01:00:00Z"),
                        message,
                        details
                );
            }
        };
    }
}
