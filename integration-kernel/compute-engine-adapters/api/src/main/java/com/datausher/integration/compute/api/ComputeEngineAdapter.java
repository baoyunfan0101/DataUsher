package com.datausher.integration.compute.api;

import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.IntegrationAdapter;

public interface ComputeEngineAdapter extends IntegrationAdapter {
    ComputeJobHandle submit(AdapterRequestContext context, ComputeJobRequest request);

    ComputeJobStatus status(AdapterRequestContext context, ComputeJobHandle handle);

    void cancel(AdapterRequestContext context, ComputeJobHandle handle);
}
