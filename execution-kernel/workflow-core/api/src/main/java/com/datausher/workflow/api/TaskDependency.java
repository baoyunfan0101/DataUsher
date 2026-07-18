package com.datausher.workflow.api;

import java.util.Objects;

public record TaskDependency(
        String upstreamTaskKey,
        String downstreamTaskKey,
        TaskDependencyCondition condition
) {
    public TaskDependency {
        upstreamTaskKey = WorkflowTaskDefinition.normalizeKey(upstreamTaskKey);
        downstreamTaskKey = WorkflowTaskDefinition.normalizeKey(downstreamTaskKey);
        condition = Objects.requireNonNull(condition, "condition must not be null");
        if (upstreamTaskKey.equals(downstreamTaskKey)) {
            throw new IllegalArgumentException("task must not depend on itself");
        }
    }
}
