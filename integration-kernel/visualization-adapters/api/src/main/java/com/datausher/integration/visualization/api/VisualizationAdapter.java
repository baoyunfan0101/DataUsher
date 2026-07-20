package com.datausher.integration.visualization.api;

import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.IntegrationAdapter;

import java.util.Optional;

public interface VisualizationAdapter extends IntegrationAdapter {
    VisualizationDatasetBinding bindDataset(
            AdapterRequestContext context,
            VisualizationDatasetBindingRequest request
    );

    VisualizationChart publishChart(
            AdapterRequestContext context,
            VisualizationChartPublicationRequest request
    );

    VisualizationDashboard publishDashboard(
            AdapterRequestContext context,
            VisualizationDashboardPublicationRequest request
    );

    Optional<VisualizationResourceRef> findResource(
            AdapterRequestContext context,
            VisualizationResourceLookup lookup
    );
}
