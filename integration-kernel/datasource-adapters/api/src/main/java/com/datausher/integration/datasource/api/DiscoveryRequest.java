package com.datausher.integration.datasource.api;

import java.util.Map;
import java.util.Objects;

public record DiscoveryRequest(
        DatasourceConnection connection,
        String namespace,
        Map<String, String> options
) {
    public DiscoveryRequest {
        connection = Objects.requireNonNull(connection, "connection must not be null");
        namespace = namespace == null ? "" : namespace.trim();
        options = options == null ? Map.of() : Map.copyOf(options);
    }
}
