package com.datausher.integration.visualization.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VisualizationContractTest {
    @Test
    void preservesOpaqueExternalReferencesAndImmutableAttributes() {
        VisualizationResourceRef datasetRef = new VisualizationResourceRef(
                "superset", "prod", VisualizationResourceType.DATASET,
                "external/dataset/42", Map.of("workspace", "analytics"));

        assertEquals("superset", datasetRef.adapterId());
        assertEquals("external/dataset/42", datasetRef.externalResourceId());
        assertThrows(UnsupportedOperationException.class,
                () -> datasetRef.attributes().put("extra", "value"));
    }

    @Test
    void requiresResourceTypesToMatchPublishedArtifacts() {
        VisualizationResourceRef datasetRef = ref(VisualizationResourceType.DATASET, "dataset-1");
        VisualizationResourceRef chartRef = ref(VisualizationResourceType.CHART, "chart-1");

        assertEquals("orders", new VisualizationDatasetBinding(
                datasetRef, "orders", "table:data.orders", "Orders", Map.of()).datasetKey());
        assertEquals("orders-by-day", new VisualizationChart(
                chartRef, "orders-by-day", "Orders by day", "bar", datasetRef, Map.of()).chartKey());
        assertEquals("sales", new VisualizationDashboard(
                ref(VisualizationResourceType.DASHBOARD, "dashboard-1"), "sales", "Sales",
                List.of(chartRef), Map.of()).dashboardKey());

        assertThrows(IllegalArgumentException.class,
                () -> new VisualizationChart(datasetRef, "bad", "Bad", "bar", datasetRef, Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new VisualizationDashboard(
                        ref(VisualizationResourceType.DASHBOARD, "dashboard-2"),
                        "bad", "Bad", List.of(datasetRef), Map.of()));
    }

    private static VisualizationResourceRef ref(VisualizationResourceType type, String externalId) {
        return new VisualizationResourceRef("superset", "prod", type, externalId, Map.of());
    }
}
