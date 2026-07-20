package com.datausher.integration.runtime.api;

import java.util.Map;

public record AdapterOperation(
        String name,
        String capabilityName,
        boolean mutating,
        Map<String, String> attributes
) {
    public AdapterOperation {
        name = IntegrationIdentifiers.normalize(name, "name");
        capabilityName = IntegrationIdentifiers.normalize(capabilityName, "capabilityName");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static AdapterOperation of(String name, String capabilityName, boolean mutating) {
        return new AdapterOperation(name, capabilityName, mutating, Map.of());
    }
}
