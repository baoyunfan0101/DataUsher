package com.datausher.workflow.core;

import com.datausher.workflow.api.WorkflowTaskDefinition;
import com.datausher.workflow.api.WorkflowVersion;

import java.util.Objects;

public record SchedulerTaskMappingRequest(
        WorkflowVersion workflowVersion,
        WorkflowTaskDefinition taskDefinition
) {
    public SchedulerTaskMappingRequest {
        workflowVersion = Objects.requireNonNull(
                workflowVersion, "workflowVersion must not be null");
        taskDefinition = Objects.requireNonNull(
                taskDefinition, "taskDefinition must not be null");
    }
}
