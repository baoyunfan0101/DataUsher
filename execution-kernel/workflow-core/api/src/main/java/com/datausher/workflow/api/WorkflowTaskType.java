package com.datausher.workflow.api;

import java.util.Locale;
import java.util.Objects;

public record WorkflowTaskType(String value) {
    public static final WorkflowTaskType EXECUTION = new WorkflowTaskType("execution");
    public static final WorkflowTaskType ADAPTER = new WorkflowTaskType("adapter");

    public WorkflowTaskType {
        value = Objects.requireNonNull(value, "value must not be null")
                .trim().toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z][a-z0-9.-]{0,126}")) {
            throw new IllegalArgumentException("value contains unsupported characters");
        }
    }
}
