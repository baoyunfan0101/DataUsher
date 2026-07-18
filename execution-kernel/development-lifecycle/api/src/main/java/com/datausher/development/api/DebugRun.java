package com.datausher.development.api;

import com.datausher.execution.api.ExecutionRequestId;
import com.datausher.execution.api.ExecutionValue;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record DebugRun(
        DebugRunId debugRunId,
        ScriptId scriptId,
        long scriptVersion,
        String idempotencyKey,
        Map<String, ExecutionValue> parameters,
        DebugRunState state,
        Optional<ExecutionRequestId> executionRequestId,
        Instant createdAt,
        Instant updatedAt,
        long revision
) {
    public DebugRun {
        debugRunId = Objects.requireNonNull(debugRunId, "debugRunId must not be null");
        scriptId = Objects.requireNonNull(scriptId, "scriptId must not be null");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null").trim();
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        state = Objects.requireNonNull(state, "state must not be null");
        executionRequestId = executionRequestId == null ? Optional.empty() : executionRequestId;
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (scriptVersion < 1 || idempotencyKey.isEmpty() || revision < 1) {
            throw new IllegalArgumentException("debug run contains invalid values");
        }
        if ((state == DebugRunState.SUBMITTED) != executionRequestId.isPresent()) {
            throw new IllegalArgumentException("submitted debug run requires executionRequestId");
        }
    }
}
