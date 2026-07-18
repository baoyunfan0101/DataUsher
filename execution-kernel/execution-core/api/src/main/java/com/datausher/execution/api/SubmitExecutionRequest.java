package com.datausher.execution.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record SubmitExecutionRequest(
        ExecutionQueueId queueId,
        ExecutionAccountId accountId,
        ExecutionWorkload workload,
        ExecutionResultMode resultMode,
        int resultPageSize,
        RequestContext requestContext
) {
    public SubmitExecutionRequest {
        queueId = Objects.requireNonNull(queueId, "queueId must not be null");
        accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        workload = Objects.requireNonNull(workload, "workload must not be null");
        resultMode = Objects.requireNonNull(resultMode, "resultMode must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (resultPageSize < 1) {
            throw new IllegalArgumentException("resultPageSize must be greater than zero");
        }
    }
}
