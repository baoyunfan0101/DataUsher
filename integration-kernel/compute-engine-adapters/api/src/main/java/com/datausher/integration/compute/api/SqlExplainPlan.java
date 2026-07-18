package com.datausher.integration.compute.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.Map;

public record SqlExplainPlan(
        String format,
        String content,
        Map<String, String> attributes
) {
    public SqlExplainPlan {
        format = IntegrationIdentifiers.normalize(format, "format");
        content = IntegrationIdentifiers.requireText(content, "content");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
