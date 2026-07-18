package com.datausher.workflow.api;

import com.datausher.platform.shared.context.RequestContext;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record ReportWorkflowTaskRunRequest(
        TaskInstanceId taskInstanceId,
        int attempt,
        WorkflowTaskRunReference runReference,
        TaskInstanceState state,
        Optional<String> failureCode,
        Instant observedAt,
        RequestContext requestContext
) {
    public ReportWorkflowTaskRunRequest {
        taskInstanceId = Objects.requireNonNull(
                taskInstanceId, "taskInstanceId must not be null");
        runReference = Objects.requireNonNull(runReference, "runReference must not be null");
        state = Objects.requireNonNull(state, "state must not be null");
        failureCode = normalize(failureCode);
        observedAt = Objects.requireNonNull(observedAt, "observedAt must not be null");
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be greater than zero");
        }
        if (state == TaskInstanceState.WAITING || state == TaskInstanceState.READY
                || state == TaskInstanceState.RETRY_WAIT || state == TaskInstanceState.SKIPPED) {
            throw new IllegalArgumentException("state is not reportable by a task runtime");
        }
    }

    private static Optional<String> normalize(Optional<String> value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        String normalized = value.orElseThrow().trim();
        return normalized.isEmpty() ? Optional.empty() : Optional.of(normalized);
    }
}
