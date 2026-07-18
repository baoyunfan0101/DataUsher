package com.datausher.integration.scheduler.api;

public enum WorkflowRunState {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    TIMED_OUT,
    UNKNOWN
}
