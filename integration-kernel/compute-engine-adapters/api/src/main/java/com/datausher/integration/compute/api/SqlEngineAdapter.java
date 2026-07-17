package com.datausher.integration.compute.api;

import com.datausher.integration.runtime.api.AdapterRequestContext;

public interface SqlEngineAdapter extends ComputeEngineAdapter {
    SqlExecutionResult execute(AdapterRequestContext context, SqlExecutionRequest request);
}
