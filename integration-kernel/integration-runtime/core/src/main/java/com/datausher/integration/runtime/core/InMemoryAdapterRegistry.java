package com.datausher.integration.runtime.core;

import com.datausher.integration.runtime.api.AdapterDescriptor;
import com.datausher.integration.runtime.api.AdapterRegistry;
import com.datausher.integration.runtime.api.AdapterType;
import com.datausher.integration.runtime.api.IntegrationAdapter;
import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryAdapterRegistry implements AdapterRegistry {
    private final ConcurrentMap<String, RegisteredAdapter> adapters = new ConcurrentHashMap<>();

    @Override
    public IntegrationAdapter register(IntegrationAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter must not be null");
        AdapterDescriptor descriptor = Objects.requireNonNull(
                adapter.descriptor(), "adapter descriptor must not be null");
        adapters.compute(descriptor.adapterId(), (adapterId, existing) -> {
            if (existing == null) {
                return new RegisteredAdapter(adapter, descriptor);
            }
            if (existing.adapter() == adapter) {
                return existing;
            }
            throw new IllegalStateException("adapter is already registered: " + adapterId);
        });
        return adapter;
    }

    @Override
    public Optional<IntegrationAdapter> unregister(String adapterId) {
        return Optional.ofNullable(adapters.remove(normalizeAdapterId(adapterId)))
                .map(RegisteredAdapter::adapter);
    }

    @Override
    public Optional<IntegrationAdapter> find(String adapterId) {
        return Optional.ofNullable(adapters.get(normalizeAdapterId(adapterId)))
                .map(RegisteredAdapter::adapter);
    }

    @Override
    public <T extends IntegrationAdapter> Optional<T> find(
            String adapterId,
            Class<T> adapterClass
    ) {
        Objects.requireNonNull(adapterClass, "adapterClass must not be null");
        return find(adapterId).filter(adapterClass::isInstance).map(adapterClass::cast);
    }

    @Override
    public List<IntegrationAdapter> findByType(AdapterType type) {
        Objects.requireNonNull(type, "type must not be null");
        return adapters.values().stream()
                .filter(registered -> registered.descriptor().type() == type)
                .sorted(adapterOrder())
                .map(RegisteredAdapter::adapter)
                .toList();
    }

    @Override
    public List<IntegrationAdapter> findByCapability(String capabilityName) {
        return adapters.values().stream()
                .filter(registered -> registered.descriptor().supports(capabilityName))
                .sorted(adapterOrder())
                .map(RegisteredAdapter::adapter)
                .toList();
    }

    @Override
    public List<IntegrationAdapter> listAdapters() {
        return adapters.values().stream()
                .sorted(adapterOrder())
                .map(RegisteredAdapter::adapter)
                .toList();
    }

    private static Comparator<RegisteredAdapter> adapterOrder() {
        return Comparator.comparing(registered -> registered.descriptor().adapterId());
    }

    private static String normalizeAdapterId(String adapterId) {
        return IntegrationIdentifiers.normalize(adapterId, "adapterId");
    }

    private record RegisteredAdapter(
            IntegrationAdapter adapter,
            AdapterDescriptor descriptor
    ) {
    }
}
