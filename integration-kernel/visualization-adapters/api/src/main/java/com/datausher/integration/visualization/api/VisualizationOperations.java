package com.datausher.integration.visualization.api;

import com.datausher.integration.runtime.api.AdapterOperation;

public final class VisualizationOperations {
    public static final AdapterOperation BIND_DATASET = AdapterOperation.of(
            "visualization.dataset.bind", VisualizationCapabilities.DATASET_BINDING, true);
    public static final AdapterOperation PUBLISH_CHART = AdapterOperation.of(
            "visualization.chart.publish", VisualizationCapabilities.CHART_PUBLICATION, true);
    public static final AdapterOperation PUBLISH_DASHBOARD = AdapterOperation.of(
            "visualization.dashboard.publish", VisualizationCapabilities.DASHBOARD_PUBLICATION, true);
    public static final AdapterOperation FIND_RESOURCE = AdapterOperation.of(
            "visualization.resource.find", VisualizationCapabilities.RESOURCE_LOOKUP, false);

    private VisualizationOperations() {
    }
}
