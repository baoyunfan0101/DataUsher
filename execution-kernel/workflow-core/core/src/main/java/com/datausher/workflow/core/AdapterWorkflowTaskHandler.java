package com.datausher.workflow.core;

import com.datausher.integration.runtime.api.AdapterOperation;
import com.datausher.workflow.api.AdapterWorkflowTaskAction;
import com.datausher.workflow.api.WorkflowTaskRunReference;

public interface AdapterWorkflowTaskHandler {
    AdapterOperation operation();

    WorkflowTaskRunReference dispatch(
            WorkflowTaskDispatchRequest request,
            AdapterWorkflowTaskAction action
    );

    default void cancel(
            WorkflowTaskCancelRequest request,
            AdapterWorkflowTaskAction action
    ) {
    }
}
