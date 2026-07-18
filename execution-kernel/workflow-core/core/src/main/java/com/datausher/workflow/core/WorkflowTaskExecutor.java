package com.datausher.workflow.core;

import com.datausher.workflow.api.WorkflowTaskRunReference;
import com.datausher.workflow.api.WorkflowTaskType;

public interface WorkflowTaskExecutor {
    WorkflowTaskType taskType();

    WorkflowTaskRunReference dispatch(WorkflowTaskDispatchRequest request);

    void cancel(WorkflowTaskCancelRequest request);
}
