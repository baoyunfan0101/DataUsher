package com.datausher.execution.api;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record ExecutionRequest(
        ExecutionRequestId requestId,
        ExecutionQueueId queueId,
        ExecutionAccountId accountId,
        ExecutionWorkload workload,
        ExecutionResultMode resultMode,
        int resultPageSize,
        ExecutionState state,
        Instant submittedAt,
        Instant updatedAt,
        Optional<ExecutionFailure> failure,
        long revision
) {
    public ExecutionRequest {
        requestId = Objects.requireNonNull(requestId, "requestId must not be null");
        queueId = Objects.requireNonNull(queueId, "queueId must not be null");
        accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        workload = Objects.requireNonNull(workload, "workload must not be null");
        resultMode = Objects.requireNonNull(resultMode, "resultMode must not be null");
        state = Objects.requireNonNull(state, "state must not be null");
        submittedAt = Objects.requireNonNull(submittedAt, "submittedAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        failure = failure == null ? Optional.empty() : failure;
        if (resultPageSize < 1) {
            throw new IllegalArgumentException("resultPageSize must be greater than zero");
        }
        if (updatedAt.isBefore(submittedAt)) {
            throw new IllegalArgumentException("updatedAt must not precede submittedAt");
        }
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be greater than zero");
        }
        if (state != ExecutionState.FAILED && state != ExecutionState.TIMED_OUT
                && failure.isPresent()) {
            throw new IllegalArgumentException("failure requires a failed or timed out state");
        }
    }
}
