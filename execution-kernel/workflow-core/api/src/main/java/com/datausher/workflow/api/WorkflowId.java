package com.datausher.workflow.api;

import java.util.Objects;

public record WorkflowId(String value) implements Comparable<WorkflowId> {
    public WorkflowId {
        value = Objects.requireNonNull(value, "value must not be null").trim();
        if (!value.matches("[a-zA-Z0-9][a-zA-Z0-9._:-]{0,254}")) {
            throw new IllegalArgumentException("value contains unsupported characters");
        }
    }

    @Override
    public int compareTo(WorkflowId other) {
        return value.compareTo(other.value);
    }
}
