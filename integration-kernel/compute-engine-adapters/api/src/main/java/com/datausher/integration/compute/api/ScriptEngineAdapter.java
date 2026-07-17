package com.datausher.integration.compute.api;

import com.datausher.integration.runtime.api.AdapterRequestContext;

public interface ScriptEngineAdapter extends ComputeEngineAdapter {
    ComputeJobHandle submitScript(
            AdapterRequestContext context,
            ScriptExecutionRequest request
    );
}
