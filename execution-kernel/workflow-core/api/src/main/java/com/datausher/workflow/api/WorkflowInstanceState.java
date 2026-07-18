package com.datausher.workflow.api;

public enum WorkflowInstanceState {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    TIMED_OUT,
    CANCELLED;

    public boolean terminal() {
        return this == SUCCEEDED || this == FAILED || this == TIMED_OUT || this == CANCELLED;
    }
}
