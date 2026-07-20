package com.datausher.integration.visualization.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.Map;
import java.util.Objects;

public record VisualizationChartPublicationRequest(
        String bindingId,
        String chartKey,
        String title,
        String chartType,
        VisualizationResourceRef datasetRef,
        Map<String, String> encoding,
        Map<String, String> options
) {
    public VisualizationChartPublicationRequest {
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        chartKey = IntegrationIdentifiers.normalize(chartKey, "chartKey");
        title = IntegrationIdentifiers.requireText(title, "title");
        chartType = IntegrationIdentifiers.normalize(chartType, "chartType");
        datasetRef = Objects.requireNonNull(datasetRef, "datasetRef must not be null");
        if (datasetRef.type() != VisualizationResourceType.DATASET) {
            throw new IllegalArgumentException("datasetRef type must be DATASET");
        }
        encoding = encoding == null ? Map.of() : Map.copyOf(encoding);
        options = options == null ? Map.of() : Map.copyOf(options);
    }
}
