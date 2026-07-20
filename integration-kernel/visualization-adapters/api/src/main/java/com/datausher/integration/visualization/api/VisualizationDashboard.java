package com.datausher.integration.visualization.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record VisualizationDashboard(
        VisualizationResourceRef ref,
        String dashboardKey,
        String title,
        List<VisualizationResourceRef> chartRefs,
        Map<String, String> attributes
) {
    public VisualizationDashboard {
        ref = Objects.requireNonNull(ref, "ref must not be null");
        if (ref.type() != VisualizationResourceType.DASHBOARD) {
            throw new IllegalArgumentException("ref type must be DASHBOARD");
        }
        dashboardKey = IntegrationIdentifiers.normalize(dashboardKey, "dashboardKey");
        title = IntegrationIdentifiers.requireText(title, "title");
        chartRefs = chartRefs == null ? List.of() : List.copyOf(chartRefs);
        if (chartRefs.isEmpty()) {
            throw new IllegalArgumentException("chartRefs must not be empty");
        }
        for (VisualizationResourceRef chartRef : chartRefs) {
            Objects.requireNonNull(chartRef, "chartRefs must not contain null");
            if (chartRef.type() != VisualizationResourceType.CHART) {
                throw new IllegalArgumentException("chartRefs must contain CHART references");
            }
        }
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
