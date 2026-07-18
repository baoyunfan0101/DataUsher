package com.datausher.integration.compute.api;

import com.datausher.integration.runtime.api.AdapterRequestContext;

public interface SqlEngineAdapter extends ComputeEngineAdapter {
    default ComputeJobHandle submitSql(
            AdapterRequestContext context,
            SqlExecutionRequest request
    ) {
        var options = new java.util.LinkedHashMap<>(request.options());
        options.put("maxRows", Integer.toString(request.maxRows()));
        return submit(context, new ComputeJobRequest(
                request.bindingId(),
                "sql",
                request.statement(),
                request.parameters(),
                options
        ));
    }

    SqlExplainPlan explain(AdapterRequestContext context, SqlExecutionRequest request);
}
