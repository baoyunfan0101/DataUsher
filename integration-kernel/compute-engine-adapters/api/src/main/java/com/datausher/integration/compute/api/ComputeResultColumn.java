package com.datausher.integration.compute.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.Map;

public record ComputeResultColumn(
        String name,
        String type,
        boolean nullable,
        Map<String, String> attributes
) {
    public ComputeResultColumn {
        name = IntegrationIdentifiers.requireText(name, "name");
        type = IntegrationIdentifiers.requireText(type, "type");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
