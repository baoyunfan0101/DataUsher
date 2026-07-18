package com.datausher.workflow.core;

import com.datausher.workflow.api.TaskInstanceState;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record SchedulerManagedTaskObservation(
        String taskKey,
        int attempt,
        TaskInstanceState state,
        Optional<String> failureCode,
        Optional<Instant> finishedAt
) {
    public SchedulerManagedTaskObservation {
        taskKey = Objects.requireNonNull(taskKey, "taskKey must not be null").trim();
        state = Objects.requireNonNull(state, "state must not be null");
        failureCode = failureCode == null ? Optional.empty() : failureCode;
        finishedAt = finishedAt == null ? Optional.empty() : finishedAt;
        if (taskKey.isEmpty() || attempt < 1) {
            throw new IllegalArgumentException("taskKey and positive attempt are required");
        }
        if (state.terminal() != finishedAt.isPresent()) {
            throw new IllegalArgumentException(
                    "finishedAt must be present exactly for terminal task states");
        }
    }
}
