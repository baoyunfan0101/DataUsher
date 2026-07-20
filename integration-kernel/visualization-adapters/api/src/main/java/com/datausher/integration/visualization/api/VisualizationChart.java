package com.datausher.integration.visualization.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.Map;
import java.util.Objects;

public record VisualizationChart(
        VisualizationResourceRef ref,
        String chartKey,
        String title,
        String chartType,
        VisualizationResourceRef datasetRef,
        Map<String, String> attributes
) {
    public VisualizationChart {
        ref = Objects.requireNonNull(ref, "ref must not be null");
        if (ref.type() != VisualizationResourceType.CHART) {
            throw new IllegalArgumentException("ref type must be CHART");
        }
        chartKey = IntegrationIdentifiers.normalize(chartKey, "chartKey");
        title = IntegrationIdentifiers.requireText(title, "title");
        chartType = IntegrationIdentifiers.normalize(chartType, "chartType");
        datasetRef = Objects.requireNonNull(datasetRef, "datasetRef must not be null");
        if (datasetRef.type() != VisualizationResourceType.DATASET) {
            throw new IllegalArgumentException("datasetRef type must be DATASET");
        }
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
