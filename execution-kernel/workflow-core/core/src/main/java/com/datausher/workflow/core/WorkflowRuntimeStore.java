package com.datausher.workflow.core;

import com.datausher.workflow.api.TaskInstanceId;
import com.datausher.workflow.api.WorkflowInstanceId;

import java.util.Optional;

public interface WorkflowRuntimeStore {
    WorkflowRunCreateResult createOrFind(StoredWorkflowRun run);

    Optional<StoredWorkflowRun> find(WorkflowInstanceId instanceId);

    Optional<StoredWorkflowRun> findByTaskInstanceId(TaskInstanceId taskInstanceId);

    void update(StoredWorkflowRun expected, StoredWorkflowRun replacement);
}
