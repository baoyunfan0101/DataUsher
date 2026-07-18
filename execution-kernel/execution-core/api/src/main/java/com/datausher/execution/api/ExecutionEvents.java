package com.datausher.execution.api;

public final class ExecutionEvents {
    public static final String SUBMITTED = "ExecutionSubmitted";
    public static final String QUEUED = "ExecutionQueued";
    public static final String STARTED = "ExecutionStarted";
    public static final String COMPLETED = "ExecutionCompleted";
    public static final String FAILED = "ExecutionFailed";
    public static final String CANCELLED = "ExecutionCancelled";
    public static final String TIMED_OUT = "ExecutionTimedOut";

    private ExecutionEvents() {
    }
}
