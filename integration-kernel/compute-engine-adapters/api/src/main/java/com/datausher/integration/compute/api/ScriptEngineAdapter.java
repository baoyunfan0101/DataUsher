package com.datausher.integration.compute.api;

import com.datausher.integration.runtime.api.AdapterRequestContext;

public interface ScriptEngineAdapter extends ComputeEngineAdapter {
    default ComputeJobHandle submitScript(
            AdapterRequestContext context,
            ScriptExecutionRequest request
    ) {
        return submit(context, new ComputeJobRequest(
                request.bindingId(),
                request.language(),
                request.script(),
                java.util.Map.of(),
                request.options()
        ));
    }
}
