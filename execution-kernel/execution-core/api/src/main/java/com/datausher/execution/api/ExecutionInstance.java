package com.datausher.execution.api;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record ExecutionInstance(
        ExecutionInstanceId instanceId,
        ExecutionRequestId requestId,
        int attempt,
        ExecutionState state,
        Instant createdAt,
        Optional<Instant> startedAt,
        Optional<Instant> finishedAt,
        Optional<ExecutionFailure> failure,
        long revision
) {
    public ExecutionInstance {
        instanceId = Objects.requireNonNull(instanceId, "instanceId must not be null");
        requestId = Objects.requireNonNull(requestId, "requestId must not be null");
        state = Objects.requireNonNull(state, "state must not be null");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        startedAt = startedAt == null ? Optional.empty() : startedAt;
        finishedAt = finishedAt == null ? Optional.empty() : finishedAt;
        failure = failure == null ? Optional.empty() : failure;
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be greater than zero");
        }
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be greater than zero");
        }
        if (startedAt.isPresent() && startedAt.get().isBefore(createdAt)) {
            throw new IllegalArgumentException("startedAt must not precede createdAt");
        }
        if (state.terminal() != finishedAt.isPresent()) {
            throw new IllegalArgumentException("finishedAt must be present exactly for terminal states");
        }
        if (state != ExecutionState.FAILED && state != ExecutionState.TIMED_OUT
                && failure.isPresent()) {
            throw new IllegalArgumentException("failure requires a failed or timed out state");
        }
    }
}
