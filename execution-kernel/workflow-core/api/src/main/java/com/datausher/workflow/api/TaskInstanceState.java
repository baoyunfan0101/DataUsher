package com.datausher.workflow.api;

public enum TaskInstanceState {
    WAITING,
    READY,
    RETRY_WAIT,
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    SKIPPED;

    public boolean terminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELLED || this == SKIPPED;
    }
}
