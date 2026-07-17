package com.datausher.integration.runtime.api;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record AdapterDescriptor(
        String adapterId,
        AdapterType type,
        String version,
        Set<AdapterCapability> capabilities,
        Map<String, String> attributes
) {
    public AdapterDescriptor {
        adapterId = IntegrationIdentifiers.normalize(adapterId, "adapterId");
        type = Objects.requireNonNull(type, "type must not be null");
        version = IntegrationIdentifiers.requireText(version, "version");
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (capabilities.isEmpty()) {
            throw new IllegalArgumentException("capabilities must not be empty");
        }
        Set<String> names = new HashSet<>();
        for (AdapterCapability capability : capabilities) {
            if (!names.add(capability.name())) {
                throw new IllegalArgumentException("duplicate capability: " + capability.name());
            }
        }
    }

    public boolean supports(String capabilityName) {
        String normalized = IntegrationIdentifiers.normalize(capabilityName, "capabilityName");
        return capabilities.stream().anyMatch(capability -> capability.name().equals(normalized));
    }
}
