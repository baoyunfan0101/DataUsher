package com.datausher.workflow.api;

import java.util.Locale;
import java.util.Objects;

public record WorkflowRuntimeType(String value) {
    public static final WorkflowRuntimeType PLATFORM_MANAGED = new WorkflowRuntimeType("platform-managed");
    public static final WorkflowRuntimeType SCHEDULER_MANAGED = new WorkflowRuntimeType("scheduler-managed");

    public WorkflowRuntimeType {
        value = Objects.requireNonNull(value, "value must not be null")
                .trim().toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z][a-z0-9.-]{0,126}")) {
            throw new IllegalArgumentException("value contains unsupported characters");
        }
    }
}
