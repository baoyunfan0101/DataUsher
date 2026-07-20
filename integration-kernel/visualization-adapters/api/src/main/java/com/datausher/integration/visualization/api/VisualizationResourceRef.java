package com.datausher.integration.visualization.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.Map;
import java.util.Objects;

public record VisualizationResourceRef(
        String adapterId,
        String bindingId,
        VisualizationResourceType type,
        String externalResourceId,
        Map<String, String> attributes
) {
    public VisualizationResourceRef {
        adapterId = IntegrationIdentifiers.normalize(adapterId, "adapterId");
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        type = Objects.requireNonNull(type, "type must not be null");
        externalResourceId = IntegrationIdentifiers.requireText(
                externalResourceId, "externalResourceId");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
