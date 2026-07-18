package com.datausher.workflow.core;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.workflow.api.TaskInstance;
import com.datausher.workflow.api.WorkflowInstance;
import com.datausher.workflow.api.WorkflowTaskDefinition;

import java.util.Objects;

public record WorkflowTaskDispatchRequest(
        WorkflowInstance workflowInstance,
        TaskInstance taskInstance,
        WorkflowTaskDefinition taskDefinition,
        RequestContext requestContext
) {
    public WorkflowTaskDispatchRequest {
        workflowInstance = Objects.requireNonNull(workflowInstance, "workflowInstance must not be null");
        taskInstance = Objects.requireNonNull(taskInstance, "taskInstance must not be null");
        taskDefinition = Objects.requireNonNull(taskDefinition, "taskDefinition must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (!workflowInstance.instanceId().equals(taskInstance.workflowInstanceId())
                || !taskInstance.taskKey().equals(taskDefinition.taskKey())) {
            throw new IllegalArgumentException("task dispatch identities must match");
        }
    }
}
