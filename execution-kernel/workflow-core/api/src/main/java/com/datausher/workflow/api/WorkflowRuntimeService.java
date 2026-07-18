package com.datausher.workflow.api;

import com.datausher.execution.api.ExecutionStateChangedEvent;

import java.util.Optional;

public interface WorkflowRuntimeService {
    WorkflowInstance trigger(TriggerWorkflowRequest request);

    WorkflowInstance cancel(CancelWorkflowRequest request);

    WorkflowInstance refresh(WorkflowInstanceId instanceId, com.datausher.platform.shared.context.RequestContext context);

    void handleExecutionStateChanged(ExecutionStateChangedEvent event);

    Optional<WorkflowInstance> findInstance(WorkflowInstanceId instanceId);
}
