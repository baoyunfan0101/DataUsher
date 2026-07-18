package com.datausher.workflow.api;

import com.datausher.execution.api.ExecutionValue;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record WorkflowInstance(
        WorkflowInstanceId instanceId,
        WorkflowId workflowId,
        long workflowVersion,
        WorkflowRuntimeBinding runtimeBinding,
        Optional<WorkflowRunReference> runReference,
        String idempotencyKey,
        Map<String, ExecutionValue> parameters,
        WorkflowInstanceState state,
        Instant createdAt,
        Instant updatedAt,
        Optional<Instant> finishedAt,
        long revision
) {
    public WorkflowInstance {
        instanceId = Objects.requireNonNull(instanceId, "instanceId must not be null");
        workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
        runtimeBinding = Objects.requireNonNull(runtimeBinding, "runtimeBinding must not be null");
        runReference = runReference == null ? Optional.empty() : runReference;
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null").trim();
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        state = Objects.requireNonNull(state, "state must not be null");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        finishedAt = finishedAt == null ? Optional.empty() : finishedAt;
        if (workflowVersion < 1 || idempotencyKey.isEmpty() || revision < 1) {
            throw new IllegalArgumentException("workflow instance contains invalid values");
        }
        if (state.terminal() != finishedAt.isPresent()) {
            throw new IllegalArgumentException("finishedAt must be present exactly for terminal states");
        }
        if (runtimeBinding.runtimeType().equals(WorkflowRuntimeType.PLATFORM_MANAGED)
                && runReference.isPresent()) {
            throw new IllegalArgumentException("platform-managed instance must not have scheduler run reference");
        }
        if (runReference.isPresent()) {
            WorkflowRunReference reference = runReference.orElseThrow();
            if (!runtimeBinding.adapterId().orElseThrow().equals(reference.adapterId())
                    || !runtimeBinding.bindingId().orElseThrow().equals(reference.bindingId())) {
                throw new IllegalArgumentException("scheduler run reference must match runtime binding");
            }
        }
    }

    public WorkflowInstance(
            WorkflowInstanceId instanceId,
            WorkflowId workflowId,
            long workflowVersion,
            String idempotencyKey,
            Map<String, ExecutionValue> parameters,
            WorkflowInstanceState state,
            Instant createdAt,
            Instant updatedAt,
            Optional<Instant> finishedAt,
            long revision
    ) {
        this(instanceId, workflowId, workflowVersion, WorkflowRuntimeBinding.PLATFORM_MANAGED,
                Optional.empty(), idempotencyKey, parameters, state, createdAt, updatedAt,
                finishedAt, revision);
    }
}
