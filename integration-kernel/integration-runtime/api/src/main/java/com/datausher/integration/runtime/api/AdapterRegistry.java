package com.datausher.integration.runtime.api;

import java.util.List;
import java.util.Optional;

public interface AdapterRegistry {
    IntegrationAdapter register(IntegrationAdapter adapter);

    Optional<IntegrationAdapter> unregister(String adapterId);

    Optional<IntegrationAdapter> find(String adapterId);

    <T extends IntegrationAdapter> Optional<T> find(String adapterId, Class<T> adapterClass);

    List<IntegrationAdapter> findByType(AdapterType type);

    List<IntegrationAdapter> findByCapability(String capabilityName);

    List<IntegrationAdapter> listAdapters();
}
