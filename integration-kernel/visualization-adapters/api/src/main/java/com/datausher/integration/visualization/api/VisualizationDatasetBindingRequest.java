package com.datausher.integration.visualization.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.Map;

public record VisualizationDatasetBindingRequest(
        String bindingId,
        String datasetKey,
        String sourceRef,
        String displayName,
        Map<String, String> attributes
) {
    public VisualizationDatasetBindingRequest {
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        datasetKey = IntegrationIdentifiers.normalize(datasetKey, "datasetKey");
        sourceRef = IntegrationIdentifiers.requireText(sourceRef, "sourceRef");
        displayName = IntegrationIdentifiers.requireText(displayName, "displayName");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
