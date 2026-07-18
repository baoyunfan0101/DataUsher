package com.datausher.integration.contract;

import com.datausher.integration.compute.api.ComputeCapabilities;
import com.datausher.integration.compute.api.ComputeEngineAdapter;
import com.datausher.integration.compute.api.ComputeJobHandle;
import com.datausher.integration.compute.api.ComputeJobRequest;
import com.datausher.integration.runtime.api.AdapterType;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ComputeEngineAdapterContract {
    private ComputeEngineAdapterContract() {
    }

    public static void verify(
            ComputeEngineAdapter adapter,
            ComputeJobRequest request,
            ComputeJobHandle handle,
            Set<String> sensitiveValues
    ) {
        IntegrationAdapterContract.verify(adapter, sensitiveValues);
        assertEquals(AdapterType.COMPUTE_ENGINE, adapter.descriptor().type());
        assertTrue(adapter.descriptor().supports(ComputeCapabilities.JOB_EXECUTION),
                "compute adapters must declare job execution capability");
        assertEquals(adapter.descriptor().adapterId(), handle.adapterId(),
                "job handles must preserve adapter identity");
        assertEquals(request.bindingId(), handle.bindingId(),
                "job handles must preserve credential binding identity");
    }
}
