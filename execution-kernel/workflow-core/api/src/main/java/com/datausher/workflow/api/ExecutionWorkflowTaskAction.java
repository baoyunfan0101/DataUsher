package com.datausher.workflow.api;

import com.datausher.execution.api.ExecutionSpecification;

import java.util.Objects;

public record ExecutionWorkflowTaskAction(
        ExecutionSpecification executionSpecification
) implements WorkflowTaskAction {
    public ExecutionWorkflowTaskAction {
        executionSpecification = Objects.requireNonNull(
                executionSpecification, "executionSpecification must not be null");
    }

    @Override
    public WorkflowTaskType taskType() {
        return WorkflowTaskType.EXECUTION;
    }
}
