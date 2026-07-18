package com.datausher.workflow.core;

import com.datausher.integration.scheduler.api.SchedulerTaskDefinition;
import com.datausher.integration.scheduler.api.SchedulerTaskType;
import com.datausher.workflow.api.ExecutionWorkflowTaskAction;
import com.datausher.workflow.api.WorkflowTaskType;

import java.util.Map;
import java.util.stream.Collectors;

public final class ExecutionSchedulerTaskDefinitionMapper
        implements SchedulerTaskDefinitionMapper {
    @Override
    public WorkflowTaskType taskType() {
        return WorkflowTaskType.EXECUTION;
    }

    @Override
    public SchedulerTaskDefinition map(SchedulerTaskMappingRequest request) {
        if (!(request.taskDefinition().action()
                instanceof ExecutionWorkflowTaskAction executionAction)) {
            throw new IllegalArgumentException("execution task action has an invalid representation");
        }
        var workload = executionAction.executionSpecification().workload();
        return new SchedulerTaskDefinition(
                request.taskDefinition().taskKey(), SchedulerTaskType.PLATFORM_EXECUTION,
                request.workflowVersion().workflowId().value() + "@"
                        + request.workflowVersion().version() + ":"
                        + request.taskDefinition().taskKey(),
                workload.parameters().entrySet().stream().collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> WorkflowIntegrationValues.convert(entry.getValue()))),
                Map.of("workloadType", workload.type().value()));
    }
}
