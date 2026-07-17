package com.datausher.integration.runtime.core;

import com.datausher.integration.runtime.api.AdapterHealth;
import com.datausher.integration.runtime.api.AdapterHealthService;
import com.datausher.integration.runtime.api.AdapterHealthStatus;
import com.datausher.integration.runtime.api.AdapterRegistry;
import com.datausher.integration.runtime.api.IntegrationAdapter;
import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DefaultAdapterHealthService implements AdapterHealthService {
    private final AdapterRegistry registry;
    private final Clock clock;

    public DefaultAdapterHealthService(AdapterRegistry registry, Clock clock) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public AdapterHealth check(String adapterId) {
        String normalized = IntegrationIdentifiers.normalize(adapterId, "adapterId");
        return registry.find(normalized)
                .map(this::probe)
                .orElseGet(() -> new AdapterHealth(
                        normalized,
                        AdapterHealthStatus.UNKNOWN,
                        clock.instant(),
                        "adapter is not registered",
                        Map.of()
                ));
    }

    @Override
    public List<AdapterHealth> checkAll() {
        return registry.listAdapters().stream().map(this::probe).toList();
    }

    private AdapterHealth probe(IntegrationAdapter adapter) {
        String adapterId = adapter.descriptor().adapterId();
        try {
            AdapterHealth health = Objects.requireNonNull(
                    adapter.checkHealth(), "adapter health must not be null");
            if (!adapterId.equals(health.adapterId())) {
                return new AdapterHealth(
                        adapterId,
                        AdapterHealthStatus.DOWN,
                        clock.instant(),
                        "adapter returned health for a different adapter",
                        Map.of("reportedAdapterId", health.adapterId())
                );
            }
            return health;
        } catch (RuntimeException failure) {
            return new AdapterHealth(
                    adapterId,
                    AdapterHealthStatus.DOWN,
                    clock.instant(),
                    "adapter health probe failed",
                    Map.of("failureType", failure.getClass().getName())
            );
        }
    }
}
