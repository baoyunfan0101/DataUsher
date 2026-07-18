package com.datausher.integration.scheduler.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record WorkflowTaskRunStatus(
        String taskKey,
        int attempt,
        WorkflowRunState state,
        Optional<Instant> startedAt,
        Optional<Instant> finishedAt,
        String message,
        Map<String, String> details
) {
    public WorkflowTaskRunStatus {
        taskKey = IntegrationIdentifiers.normalize(taskKey, "taskKey");
        state = Objects.requireNonNull(state, "state must not be null");
        startedAt = startedAt == null ? Optional.empty() : startedAt;
        finishedAt = finishedAt == null ? Optional.empty() : finishedAt;
        message = message == null ? "" : message.trim();
        details = details == null ? Map.of() : Map.copyOf(details);
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be greater than zero");
        }
        if (finishedAt.isPresent() != terminal(state)) {
            throw new IllegalArgumentException("finishedAt must be present exactly for terminal states");
        }
    }

    private static boolean terminal(WorkflowRunState state) {
        return state == WorkflowRunState.SUCCEEDED || state == WorkflowRunState.FAILED
                || state == WorkflowRunState.CANCELLED || state == WorkflowRunState.TIMED_OUT;
    }
}
