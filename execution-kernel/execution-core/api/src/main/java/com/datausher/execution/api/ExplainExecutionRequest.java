package com.datausher.execution.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record ExplainExecutionRequest(
        ExecutionAccountId accountId,
        ExecutionWorkload workload,
        RequestContext requestContext
) {
    public ExplainExecutionRequest {
        accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        workload = Objects.requireNonNull(workload, "workload must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
    }
}
