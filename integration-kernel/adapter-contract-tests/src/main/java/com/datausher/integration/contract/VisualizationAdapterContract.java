package com.datausher.integration.contract;

import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.AdapterType;
import com.datausher.integration.visualization.api.VisualizationAdapter;
import com.datausher.integration.visualization.api.VisualizationCapabilities;
import com.datausher.integration.visualization.api.VisualizationDatasetBinding;
import com.datausher.integration.visualization.api.VisualizationDatasetBindingRequest;
import com.datausher.integration.visualization.api.VisualizationResourceType;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class VisualizationAdapterContract {
    private VisualizationAdapterContract() {
    }

    public static VisualizationDatasetBinding verifyDatasetBinding(
            VisualizationAdapter adapter,
            AdapterRequestContext context,
            VisualizationDatasetBindingRequest request,
            Set<String> sensitiveValues
    ) {
        IntegrationAdapterContract.verify(adapter, sensitiveValues);
        assertNotNull(context, "context must not be null");
        assertNotNull(request, "request must not be null");
        assertEquals(AdapterType.VISUALIZATION, adapter.descriptor().type());
        assertTrue(adapter.descriptor().supports(VisualizationCapabilities.DATASET_BINDING),
                "visualization adapters must declare dataset binding capability");

        VisualizationDatasetBinding binding = adapter.bindDataset(context, request);
        assertNotNull(binding, "dataset binding must not be null");
        assertEquals(VisualizationResourceType.DATASET, binding.ref().type());
        assertEquals(adapter.descriptor().adapterId(), binding.ref().adapterId());
        assertEquals(request.bindingId(), binding.ref().bindingId());
        assertEquals(request.datasetKey(), binding.datasetKey());
        assertEquals(request.sourceRef(), binding.sourceRef());
        return binding;
    }
}
