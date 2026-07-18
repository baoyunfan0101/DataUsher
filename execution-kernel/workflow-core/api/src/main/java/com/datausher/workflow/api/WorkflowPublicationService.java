package com.datausher.workflow.api;

import java.util.Optional;

public interface WorkflowPublicationService {
    WorkflowPublication publish(PublishWorkflowRequest request);

    Optional<WorkflowPublication> findPublication(WorkflowId workflowId, long version);
}
