package com.datausher.workflow.core;

import com.datausher.workflow.api.TaskInstanceId;
import com.datausher.workflow.api.WorkflowInstanceId;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WorkflowRuntimeStore {
    WorkflowRunCreateResult createOrFind(StoredWorkflowRun run);

    Optional<StoredWorkflowRun> find(WorkflowInstanceId instanceId);

    Optional<StoredWorkflowRun> findByTaskInstanceId(TaskInstanceId taskInstanceId);

    List<WorkflowRunLease> claimRunnable(
            String workerId,
            Instant now,
            Duration leaseDuration,
            int limit
    );

    Optional<StoredWorkflowRun> findClaimed(WorkflowRunLease lease, Instant now);

    WorkflowRunLease renew(
            WorkflowRunLease lease,
            Instant now,
            Duration leaseDuration
    );

    void release(WorkflowRunLease lease);

    void update(StoredWorkflowRun expected, StoredWorkflowRun replacement);
}
