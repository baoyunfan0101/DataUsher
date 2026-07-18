package com.datausher.workflow.api;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record WorkflowRuntimeBinding(
        WorkflowRuntimeType runtimeType,
        Optional<String> adapterId,
        Optional<String> bindingId,
        Map<String, String> options
) {
    public static final WorkflowRuntimeBinding PLATFORM_MANAGED = new WorkflowRuntimeBinding(
            WorkflowRuntimeType.PLATFORM_MANAGED, Optional.empty(), Optional.empty(), Map.of());

    public WorkflowRuntimeBinding {
        runtimeType = Objects.requireNonNull(runtimeType, "runtimeType must not be null");
        adapterId = normalize(adapterId);
        bindingId = normalize(bindingId);
        options = options == null ? Map.of() : Map.copyOf(options);
        boolean schedulerManaged = runtimeType.equals(WorkflowRuntimeType.SCHEDULER_MANAGED);
        if (schedulerManaged != (adapterId.isPresent() && bindingId.isPresent())) {
            throw new IllegalArgumentException(
                    "scheduler-managed runtime requires adapterId and bindingId");
        }
        if (!schedulerManaged && (adapterId.isPresent() || bindingId.isPresent())) {
            throw new IllegalArgumentException(
                    "platform-managed runtime must not declare scheduler binding");
        }
    }

    public static WorkflowRuntimeBinding schedulerManaged(
            String adapterId,
            String bindingId,
            Map<String, String> options
    ) {
        return new WorkflowRuntimeBinding(
                WorkflowRuntimeType.SCHEDULER_MANAGED,
                Optional.of(adapterId), Optional.of(bindingId), options);
    }

    private static Optional<String> normalize(Optional<String> value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        String normalized = value.orElseThrow().trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("runtime binding identifiers must not be blank");
        }
        return Optional.of(normalized);
    }
}
