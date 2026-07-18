package com.datausher.workflow.api;

public interface WorkflowCommandService {
    WorkflowDefinition create(CreateWorkflowRequest request);

    WorkflowVersion createVersion(CreateWorkflowVersionRequest request);
}
