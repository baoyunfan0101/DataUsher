package com.datausher.workflow.core;

public final class WorkflowRunLeaseLostException extends IllegalStateException {
    public WorkflowRunLeaseLostException(String message) {
        super(message);
    }
}
