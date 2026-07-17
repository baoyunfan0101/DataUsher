package com.datausher.integration.runtime.api;

import java.util.Map;

public record AdapterCapability(String name, Map<String, String> attributes) {
    public AdapterCapability {
        name = IntegrationIdentifiers.normalize(name, "name");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static AdapterCapability of(String name) {
        return new AdapterCapability(name, Map.of());
    }
}
