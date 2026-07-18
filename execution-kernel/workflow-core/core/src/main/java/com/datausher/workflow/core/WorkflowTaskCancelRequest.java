package com.datausher.workflow.core;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.workflow.api.TaskInstance;
import com.datausher.workflow.api.WorkflowTaskDefinition;

import java.util.Objects;

public record WorkflowTaskCancelRequest(
        TaskInstance taskInstance,
        WorkflowTaskDefinition taskDefinition,
        RequestContext requestContext
) {
    public WorkflowTaskCancelRequest {
        taskInstance = Objects.requireNonNull(taskInstance, "taskInstance must not be null");
        taskDefinition = Objects.requireNonNull(taskDefinition, "taskDefinition must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (!taskInstance.taskKey().equals(taskDefinition.taskKey())) {
            throw new IllegalArgumentException("task cancellation identities must match");
        }
    }
}
