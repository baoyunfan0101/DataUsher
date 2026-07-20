package com.datausher.integration.datasource.jdbc;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.Map;

public record JdbcRelationalAdapterConfig(
        String adapterId,
        String vendor,
        String version,
        Map<String, String> attributes
) {
    public JdbcRelationalAdapterConfig {
        adapterId = IntegrationIdentifiers.normalize(adapterId, "adapterId");
        vendor = IntegrationIdentifiers.normalize(vendor, "vendor");
        version = IntegrationIdentifiers.requireText(version, "version");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
