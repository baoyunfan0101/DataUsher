package com.datausher.workflow.core;

import com.datausher.workflow.api.WorkflowInstanceState;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record SchedulerManagedWorkflowObservation(
        Optional<WorkflowInstanceState> state,
        Instant observedAt,
        List<SchedulerManagedTaskObservation> tasks
) {
    public SchedulerManagedWorkflowObservation {
        state = state == null ? Optional.empty() : state;
        observedAt = Objects.requireNonNull(observedAt, "observedAt must not be null");
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }
}
