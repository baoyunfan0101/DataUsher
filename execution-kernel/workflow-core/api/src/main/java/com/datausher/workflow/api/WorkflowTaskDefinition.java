package com.datausher.workflow.api;

import com.datausher.execution.api.ExecutionSpecification;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record WorkflowTaskDefinition(
        String taskKey,
        String displayName,
        WorkflowTaskAction action,
        TaskRetryPolicy retryPolicy,
        Duration timeout,
        Map<String, String> attributes
) {
    public WorkflowTaskDefinition {
        taskKey = normalizeKey(taskKey);
        displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        action = Objects.requireNonNull(action, "action must not be null");
        retryPolicy = retryPolicy == null ? TaskRetryPolicy.NONE : retryPolicy;
        timeout = Objects.requireNonNull(timeout, "timeout must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (displayName.isEmpty() || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("displayName and positive timeout are required");
        }
    }

    public WorkflowTaskDefinition(
            String taskKey,
            String displayName,
            ExecutionSpecification executionSpecification,
            TaskRetryPolicy retryPolicy,
            Duration timeout,
            Map<String, String> attributes
    ) {
        this(taskKey, displayName, new ExecutionWorkflowTaskAction(executionSpecification),
                retryPolicy, timeout, attributes);
    }

    public ExecutionSpecification executionSpecification() {
        if (action instanceof ExecutionWorkflowTaskAction executionAction) {
            return executionAction.executionSpecification();
        }
        throw new IllegalStateException("workflow task is not an execution action: " + action.taskType());
    }

    static String normalizeKey(String value) {
        String normalized = Objects.requireNonNull(value, "taskKey must not be null")
                .trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z][a-z0-9.-]{0,126}")) {
            throw new IllegalArgumentException("taskKey must match [a-z][a-z0-9.-]{0,126}");
        }
        return normalized;
    }
}
