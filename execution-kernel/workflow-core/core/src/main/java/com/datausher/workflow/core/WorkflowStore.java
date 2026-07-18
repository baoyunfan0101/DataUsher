package com.datausher.workflow.core;

import com.datausher.workflow.api.WorkflowDefinition;
import com.datausher.workflow.api.WorkflowId;
import com.datausher.workflow.api.WorkflowVersion;

import java.util.List;
import java.util.Optional;

public interface WorkflowStore {
    void createWorkflow(WorkflowDefinition workflow);

    void deleteWorkflow(WorkflowDefinition workflow);

    void createVersion(
            WorkflowDefinition expectedWorkflow,
            WorkflowDefinition updatedWorkflow,
            WorkflowVersion version
    );

    void deleteVersion(
            WorkflowDefinition expectedWorkflow,
            WorkflowDefinition restoredWorkflow,
            WorkflowVersion version
    );

    Optional<WorkflowDefinition> findWorkflow(WorkflowId workflowId);

    Optional<WorkflowVersion> findVersion(WorkflowId workflowId, long version);

    List<WorkflowVersion> listVersions(WorkflowId workflowId);
}
