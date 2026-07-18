package com.datausher.workflow.api;

import java.util.Optional;

public interface WorkflowRuntimeService {
    WorkflowInstance trigger(TriggerWorkflowRequest request);

    WorkflowInstance cancel(CancelWorkflowRequest request);

    WorkflowInstance refresh(WorkflowInstanceId instanceId, com.datausher.platform.shared.context.RequestContext context);

    WorkflowInstance reportTaskRun(ReportWorkflowTaskRunRequest request);

    Optional<WorkflowInstance> findInstance(WorkflowInstanceId instanceId);
}
