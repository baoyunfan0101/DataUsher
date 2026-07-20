package com.datausher.integration.visualization.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.Map;
import java.util.Objects;

public record VisualizationDatasetBinding(
        VisualizationResourceRef ref,
        String datasetKey,
        String sourceRef,
        String displayName,
        Map<String, String> attributes
) {
    public VisualizationDatasetBinding {
        ref = Objects.requireNonNull(ref, "ref must not be null");
        if (ref.type() != VisualizationResourceType.DATASET) {
            throw new IllegalArgumentException("ref type must be DATASET");
        }
        datasetKey = IntegrationIdentifiers.normalize(datasetKey, "datasetKey");
        sourceRef = IntegrationIdentifiers.requireText(sourceRef, "sourceRef");
        displayName = IntegrationIdentifiers.requireText(displayName, "displayName");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
