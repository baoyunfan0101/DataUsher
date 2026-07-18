package com.datausher.workflow.core;

import com.datausher.workflow.api.WorkflowId;
import com.datausher.workflow.api.WorkflowPublication;

import java.util.Optional;

public interface WorkflowPublicationStore {
    WorkflowPublication createOrFind(WorkflowPublication publication);

    Optional<WorkflowPublication> find(WorkflowId workflowId, long version);
}
