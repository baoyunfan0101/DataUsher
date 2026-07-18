package com.datausher.workflow.api;

public enum TaskInstanceState {
    WAITING,
    READY,
    RETRY_WAIT,
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    TIMED_OUT,
    CANCELLED,
    SKIPPED;

    public boolean terminal() {
        return this == SUCCEEDED || this == FAILED || this == TIMED_OUT
                || this == CANCELLED || this == SKIPPED;
    }
}
