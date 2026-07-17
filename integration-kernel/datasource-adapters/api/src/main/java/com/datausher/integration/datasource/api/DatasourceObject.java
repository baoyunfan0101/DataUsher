package com.datausher.integration.datasource.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.Map;

public record DatasourceObject(String name, String kind, Map<String, String> attributes) {
    public DatasourceObject {
        name = IntegrationIdentifiers.requireText(name, "name");
        kind = IntegrationIdentifiers.normalize(kind, "kind");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
