package com.datausher.workflow.core;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.workflow.api.WorkflowInstance;
import com.datausher.workflow.api.WorkflowInstanceId;

public interface WorkflowWorker {
    WorkflowInstance dispatchReady(WorkflowInstanceId instanceId, RequestContext requestContext);
}
