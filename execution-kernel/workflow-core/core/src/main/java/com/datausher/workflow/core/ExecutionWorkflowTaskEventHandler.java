package com.datausher.workflow.core;

import com.datausher.execution.api.ExecutionStateChangedEvent;

public interface ExecutionWorkflowTaskEventHandler {
    void handleExecutionStateChanged(ExecutionStateChangedEvent event);
}
