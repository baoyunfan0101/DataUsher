package com.datausher.integration.visualization.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.Map;
import java.util.Objects;

public record VisualizationResourceLookup(
        String bindingId,
        VisualizationResourceType type,
        String resourceKey,
        Map<String, String> attributes
) {
    public VisualizationResourceLookup {
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        type = Objects.requireNonNull(type, "type must not be null");
        resourceKey = IntegrationIdentifiers.normalize(resourceKey, "resourceKey");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
