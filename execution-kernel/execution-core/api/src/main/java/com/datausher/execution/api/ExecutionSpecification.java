package com.datausher.execution.api;

import java.util.Objects;

public record ExecutionSpecification(
        ExecutionQueueId queueId,
        ExecutionAccountId accountId,
        ExecutionWorkload workload,
        ExecutionResultMode resultMode,
        int resultPageSize
) {
    public ExecutionSpecification {
        queueId = Objects.requireNonNull(queueId, "queueId must not be null");
        accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        workload = Objects.requireNonNull(workload, "workload must not be null");
        resultMode = Objects.requireNonNull(resultMode, "resultMode must not be null");
        if (resultPageSize < 1) {
            throw new IllegalArgumentException("resultPageSize must be greater than zero");
        }
    }
}
