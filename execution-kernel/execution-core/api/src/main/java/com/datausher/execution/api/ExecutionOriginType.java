package com.datausher.execution.api;

import java.util.Objects;

public record ExecutionOriginType(String value) {
    public static final ExecutionOriginType DIRECT = new ExecutionOriginType("direct");
    public static final ExecutionOriginType WORKFLOW_TASK = new ExecutionOriginType("workflow-task");
    public static final ExecutionOriginType DEBUG_RUN = new ExecutionOriginType("debug-run");

    public ExecutionOriginType {
        value = Objects.requireNonNull(value, "value must not be null").trim().toLowerCase();
        if (!value.matches("[a-z][a-z0-9.-]{0,126}")) {
            throw new IllegalArgumentException("value must match [a-z][a-z0-9.-]{0,126}");
        }
    }
}
