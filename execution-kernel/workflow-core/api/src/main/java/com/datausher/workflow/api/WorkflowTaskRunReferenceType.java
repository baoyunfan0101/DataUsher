package com.datausher.workflow.api;

import java.util.Locale;
import java.util.Objects;

public record WorkflowTaskRunReferenceType(String value) {
    public static final WorkflowTaskRunReferenceType EXECUTION =
            new WorkflowTaskRunReferenceType("execution");
    public static final WorkflowTaskRunReferenceType ADAPTER =
            new WorkflowTaskRunReferenceType("adapter");

    public WorkflowTaskRunReferenceType {
        value = Objects.requireNonNull(value, "value must not be null")
                .trim().toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z][a-z0-9.-]{0,126}")) {
            throw new IllegalArgumentException("value contains unsupported characters");
        }
    }
}
