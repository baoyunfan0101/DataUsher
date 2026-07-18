package com.datausher.execution.api;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record ExecutionRequest(
        ExecutionRequestId requestId,
        ExecutionSpecification specification,
        String idempotencyKey,
        ExecutionOrigin origin,
        ExecutionState state,
        Instant submittedAt,
        Instant updatedAt,
        Optional<ExecutionFailure> failure,
        long revision
) {
    public ExecutionRequest {
        requestId = Objects.requireNonNull(requestId, "requestId must not be null");
        specification = Objects.requireNonNull(specification, "specification must not be null");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null").trim();
        origin = Objects.requireNonNull(origin, "origin must not be null");
        state = Objects.requireNonNull(state, "state must not be null");
        submittedAt = Objects.requireNonNull(submittedAt, "submittedAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        failure = failure == null ? Optional.empty() : failure;
        if (idempotencyKey.isEmpty()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
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

    public ExecutionQueueId queueId() {
        return specification.queueId();
    }

    public ExecutionAccountId accountId() {
        return specification.accountId();
    }

    public ExecutionWorkload workload() {
        return specification.workload();
    }

    public ExecutionResultMode resultMode() {
        return specification.resultMode();
    }

    public int resultPageSize() {
        return specification.resultPageSize();
    }
}
