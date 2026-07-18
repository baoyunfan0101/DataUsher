package com.datausher.workflow.api;

import java.util.List;
import java.util.Optional;

public interface WorkflowQueryService {
    Optional<WorkflowDefinition> findWorkflow(WorkflowId workflowId);

    Optional<WorkflowVersion> findVersion(WorkflowId workflowId, long version);

    Optional<WorkflowVersion> findLatestVersion(WorkflowId workflowId);

    List<WorkflowVersion> listVersions(WorkflowId workflowId);
}
