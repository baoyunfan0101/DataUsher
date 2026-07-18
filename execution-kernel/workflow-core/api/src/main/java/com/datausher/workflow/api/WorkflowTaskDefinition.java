package com.datausher.workflow.api;

import com.datausher.execution.api.ExecutionSpecification;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public record WorkflowTaskDefinition(
        String taskKey,
        String displayName,
        ExecutionSpecification executionSpecification,
        TaskRetryPolicy retryPolicy,
        Duration timeout,
        Map<String, String> attributes
) {
    public WorkflowTaskDefinition {
        taskKey = normalizeKey(taskKey);
        displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        executionSpecification = Objects.requireNonNull(
                executionSpecification, "executionSpecification must not be null");
        retryPolicy = retryPolicy == null ? TaskRetryPolicy.NONE : retryPolicy;
        timeout = Objects.requireNonNull(timeout, "timeout must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (displayName.isEmpty() || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("displayName and positive timeout are required");
        }
    }

    static String normalizeKey(String value) {
        String normalized = Objects.requireNonNull(value, "taskKey must not be null").trim().toLowerCase();
        if (!normalized.matches("[a-z][a-z0-9.-]{0,126}")) {
            throw new IllegalArgumentException("taskKey must match [a-z][a-z0-9.-]{0,126}");
        }
        return normalized;
    }
}
