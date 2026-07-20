package com.datausher.data.quality.api;

import com.datausher.execution.api.ExecutionAccountId;
import com.datausher.execution.api.ExecutionQueueId;

import java.util.Map;
import java.util.Objects;

public record DataExecutionPolicy(
        AssessmentExecutionType executionType,
        ExecutionQueueId queueId,
        ExecutionAccountId accountId,
        int resultPageSize,
        Map<String, String> options
) {
    public DataExecutionPolicy {
        executionType = Objects.requireNonNull(executionType, "executionType must not be null");
        queueId = Objects.requireNonNull(queueId, "queueId must not be null");
        accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        options = QualityValues.attributes(options);
        if (resultPageSize < 1) {
            throw new IllegalArgumentException("resultPageSize must be greater than zero");
        }
    }

    public DataExecutionPolicy(
            ExecutionQueueId queueId,
            ExecutionAccountId accountId,
            int resultPageSize,
            Map<String, String> options
    ) {
        this(AssessmentExecutionType.COMPUTE, queueId, accountId, resultPageSize, options);
    }
}
