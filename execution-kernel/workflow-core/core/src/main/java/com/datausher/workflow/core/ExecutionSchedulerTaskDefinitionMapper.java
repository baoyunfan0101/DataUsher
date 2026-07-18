package com.datausher.workflow.core;

import com.datausher.execution.api.ExecutionValue;
import com.datausher.integration.runtime.api.IntegrationValue;
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
                        Map.Entry::getKey, entry -> toIntegrationValue(entry.getValue()))),
                Map.of("workloadType", workload.type().value()));
    }

    private static IntegrationValue toIntegrationValue(ExecutionValue value) {
        return switch (value) {
            case ExecutionValue.NullValue ignored -> new IntegrationValue.NullValue();
            case ExecutionValue.TextValue text -> new IntegrationValue.TextValue(text.value());
            case ExecutionValue.BooleanValue bool -> new IntegrationValue.BooleanValue(bool.value());
            case ExecutionValue.DecimalValue decimal -> new IntegrationValue.DecimalValue(decimal.value());
            case ExecutionValue.InstantValue instant -> new IntegrationValue.InstantValue(instant.value());
            case ExecutionValue.DateValue date -> new IntegrationValue.DateValue(date.value());
            case ExecutionValue.DateTimeValue dateTime -> new IntegrationValue.DateTimeValue(dateTime.value());
            case ExecutionValue.BinaryValue binary -> new IntegrationValue.BinaryValue(binary.base64());
            case ExecutionValue.ArrayValue array -> new IntegrationValue.ArrayValue(
                    array.values().stream().map(
                            ExecutionSchedulerTaskDefinitionMapper::toIntegrationValue).toList());
            case ExecutionValue.ObjectValue object -> new IntegrationValue.ObjectValue(
                    object.values().entrySet().stream().collect(Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            entry -> toIntegrationValue(entry.getValue()))));
        };
    }
}
