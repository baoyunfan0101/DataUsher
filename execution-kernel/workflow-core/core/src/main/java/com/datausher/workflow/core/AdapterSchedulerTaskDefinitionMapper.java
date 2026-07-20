package com.datausher.workflow.core;

import com.datausher.integration.scheduler.api.SchedulerTaskDefinition;
import com.datausher.integration.scheduler.api.SchedulerTaskType;
import com.datausher.workflow.api.AdapterWorkflowTaskAction;
import com.datausher.workflow.api.WorkflowTaskType;

import java.util.Map;

public final class AdapterSchedulerTaskDefinitionMapper implements SchedulerTaskDefinitionMapper {
    @Override
    public WorkflowTaskType taskType() {
        return WorkflowTaskType.ADAPTER;
    }

    @Override
    public SchedulerTaskDefinition map(SchedulerTaskMappingRequest request) {
        if (!(request.taskDefinition().action() instanceof AdapterWorkflowTaskAction action)) {
            throw new IllegalArgumentException("adapter task action has an invalid representation");
        }
        return new SchedulerTaskDefinition(
                request.taskDefinition().taskKey(),
                SchedulerTaskType.PLATFORM_ADAPTER,
                request.workflowVersion().workflowId().value() + "@"
                        + request.workflowVersion().version() + ":"
                        + request.taskDefinition().taskKey(),
                action.parameters(),
                Map.of(
                        "adapterId", action.adapterId(),
                        "bindingId", action.bindingId(),
                        "operation", action.operation().name(),
                        "capability", action.operation().capabilityName(),
                        "mutating", Boolean.toString(action.operation().mutating()),
                        "idempotencyKey", action.idempotencyKey()
                )
        );
    }
}
