package com.datausher.integration.datasource.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.Map;

public record DatasourceConnection(String bindingId, Map<String, String> options) {
    public DatasourceConnection {
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        options = options == null ? Map.of() : Map.copyOf(options);
    }
}
