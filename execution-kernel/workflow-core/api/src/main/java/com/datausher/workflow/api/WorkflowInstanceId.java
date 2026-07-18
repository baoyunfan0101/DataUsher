package com.datausher.workflow.api;

import java.util.Objects;

public record WorkflowInstanceId(String value) implements Comparable<WorkflowInstanceId> {
    public WorkflowInstanceId {
        value = Objects.requireNonNull(value, "value must not be null").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    @Override
    public int compareTo(WorkflowInstanceId other) {
        return value.compareTo(other.value);
    }
}
