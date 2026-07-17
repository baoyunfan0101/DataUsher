package com.datausher.integration.runtime.core;

import com.datausher.integration.runtime.api.AdapterCapability;
import com.datausher.integration.runtime.api.AdapterDescriptor;
import com.datausher.integration.runtime.api.AdapterHealth;
import com.datausher.integration.runtime.api.AdapterHealthStatus;
import com.datausher.integration.runtime.api.AdapterType;
import com.datausher.integration.runtime.api.IntegrationAdapter;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultAdapterHealthServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");

    @Test
    void isolatesProbeFailuresAndReportsMissingAdapters() {
        InMemoryAdapterRegistry registry = new InMemoryAdapterRegistry();
        registry.register(new FailingAdapter());
        DefaultAdapterHealthService service = new DefaultAdapterHealthService(
                registry, Clock.fixed(NOW, ZoneOffset.UTC));

        AdapterHealth failed = service.check("failing");

        assertEquals(AdapterHealthStatus.DOWN, failed.status());
        assertEquals(NOW, failed.checkedAt());
        assertEquals("adapter health probe failed", failed.message());
        assertEquals(AdapterHealthStatus.UNKNOWN, service.check("missing").status());
    }

    private static final class FailingAdapter implements IntegrationAdapter {
        @Override
        public AdapterDescriptor descriptor() {
            return new AdapterDescriptor(
                    "failing",
                    AdapterType.COMPUTE_ENGINE,
                    "1.0",
                    Set.of(AdapterCapability.of("compute.job.execute")),
                    Map.of()
            );
        }

        @Override
        public AdapterHealth checkHealth() {
            throw new IllegalStateException("probe failed");
        }
    }
}
