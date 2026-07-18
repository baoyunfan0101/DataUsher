package com.datausher.workflow.core;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.workflow.api.WorkflowInstance;
import com.datausher.workflow.api.WorkflowInstanceId;

import java.time.Duration;
import java.util.List;

public interface WorkflowWorker {
    WorkflowInstance dispatchReady(WorkflowInstanceId instanceId, RequestContext requestContext);

    List<WorkflowRunLease> claimRunnable(String workerId, Duration leaseDuration, int limit);

    WorkflowInstance dispatchClaimed(
            WorkflowRunLease lease,
            RequestContext requestContext
    );

    WorkflowRunLease renew(WorkflowRunLease lease, Duration leaseDuration);

    void release(WorkflowRunLease lease);
}
