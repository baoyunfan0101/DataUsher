package com.datausher.workflow.api;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record TaskInstance(
        TaskInstanceId taskInstanceId,
        WorkflowInstanceId workflowInstanceId,
        String taskKey,
        int attempt,
        TaskInstanceState state,
        Optional<WorkflowTaskRunReference> runReference,
        Optional<Instant> nextEligibleAt,
        Optional<Instant> deadlineAt,
        Optional<String> failureCode,
        Instant createdAt,
        Instant updatedAt,
        Optional<Instant> finishedAt,
        long revision
) {
    public TaskInstance {
        taskInstanceId = Objects.requireNonNull(taskInstanceId, "taskInstanceId must not be null");
        workflowInstanceId = Objects.requireNonNull(
                workflowInstanceId, "workflowInstanceId must not be null");
        taskKey = WorkflowTaskDefinition.normalizeKey(taskKey);
        state = Objects.requireNonNull(state, "state must not be null");
        runReference = runReference == null ? Optional.empty() : runReference;
        nextEligibleAt = nextEligibleAt == null ? Optional.empty() : nextEligibleAt;
        deadlineAt = deadlineAt == null ? Optional.empty() : deadlineAt;
        failureCode = failureCode == null ? Optional.empty() : failureCode;
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        finishedAt = finishedAt == null ? Optional.empty() : finishedAt;
        if (attempt < 1 || revision < 1) {
            throw new IllegalArgumentException("attempt and revision must be greater than zero");
        }
        if (state.terminal() != finishedAt.isPresent()) {
            throw new IllegalArgumentException("finishedAt must be present exactly for terminal states");
        }
        if (state != TaskInstanceState.RETRY_WAIT && nextEligibleAt.isPresent()) {
            throw new IllegalArgumentException("nextEligibleAt requires retry wait state");
        }
    }

    public TaskInstance(
            TaskInstanceId taskInstanceId,
            WorkflowInstanceId workflowInstanceId,
            String taskKey,
            int attempt,
            TaskInstanceState state,
            Optional<com.datausher.execution.api.ExecutionRequestId> executionRequestId,
            Optional<Instant> nextEligibleAt,
            Optional<String> failureCode,
            Instant createdAt,
            Instant updatedAt,
            Optional<Instant> finishedAt,
            long revision
    ) {
        this(taskInstanceId, workflowInstanceId, taskKey, attempt, state,
                executionRequestId == null ? Optional.empty() : executionRequestId.map(value ->
                        new WorkflowTaskRunReference(
                                WorkflowTaskRunReferenceType.EXECUTION, value.value(), java.util.Map.of())),
                nextEligibleAt, Optional.empty(), failureCode, createdAt, updatedAt, finishedAt, revision);
    }

    public Optional<com.datausher.execution.api.ExecutionRequestId> executionRequestId() {
        return runReference
                .filter(value -> value.type().equals(WorkflowTaskRunReferenceType.EXECUTION))
                .map(value -> new com.datausher.execution.api.ExecutionRequestId(value.referenceId()));
    }
}
