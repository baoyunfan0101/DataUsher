package com.datausher.workflow.core;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.workflow.api.TaskInstance;
import com.datausher.workflow.api.WorkflowInstance;

import java.util.List;
import java.util.Objects;

public record StoredWorkflowRun(
        WorkflowInstance instance,
        List<TaskInstance> tasks,
        RequestContext requestContext
) {
    public StoredWorkflowRun {
        instance = Objects.requireNonNull(instance, "instance must not be null");
        tasks = List.copyOf(tasks);
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
    }
}
