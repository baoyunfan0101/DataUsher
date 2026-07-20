package com.datausher.workflow.core;

import com.datausher.workflow.api.AdapterWorkflowTaskAction;
import com.datausher.workflow.api.WorkflowTaskRunReference;
import com.datausher.workflow.api.WorkflowTaskType;

import java.util.Objects;

public final class AdapterWorkflowTaskExecutor implements WorkflowTaskExecutor {
    private final AdapterWorkflowTaskHandlerRegistry handlers;

    public AdapterWorkflowTaskExecutor(AdapterWorkflowTaskHandlerRegistry handlers) {
        this.handlers = Objects.requireNonNull(handlers, "handlers must not be null");
    }

    @Override
    public WorkflowTaskType taskType() {
        return WorkflowTaskType.ADAPTER;
    }

    @Override
    public WorkflowTaskRunReference dispatch(WorkflowTaskDispatchRequest request) {
        AdapterWorkflowTaskAction action = requireAction(request.taskDefinition().action());
        return handlers.require(action.operation()).dispatch(request, action);
    }

    @Override
    public void cancel(WorkflowTaskCancelRequest request) {
        AdapterWorkflowTaskAction action = requireAction(request.taskDefinition().action());
        handlers.require(action.operation()).cancel(request, action);
    }

    private static AdapterWorkflowTaskAction requireAction(Object action) {
        if (!(action instanceof AdapterWorkflowTaskAction adapterAction)) {
            throw new IllegalArgumentException("adapter task requires AdapterWorkflowTaskAction");
        }
        return adapterAction;
    }
}
