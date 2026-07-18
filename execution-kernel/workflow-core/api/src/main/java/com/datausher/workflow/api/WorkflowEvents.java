package com.datausher.workflow.api;

public final class WorkflowEvents {
    public static final String CREATED = "workflow.created";
    public static final String VERSION_CREATED = "workflow.version-created";
    public static final String PUBLISHED = "workflow.published";
    public static final String TRIGGERED = "workflow.triggered";
    public static final String INSTANCE_STATE_CHANGED = "workflow.instance-state-changed";
    public static final String TASK_READY = "workflow.task-ready";
    public static final String TASK_SUBMITTED = "workflow.task-submitted";
    public static final String TASK_STARTED = "workflow.task-started";
    public static final String TASK_RETRY_WAIT = "workflow.task-retry-wait";
    public static final String TASK_COMPLETED = "workflow.task-completed";
    public static final String TASK_FAILED = "workflow.task-failed";
    public static final String TASK_TIMED_OUT = "workflow.task-timed-out";
    public static final String TASK_CANCELLED = "workflow.task-cancelled";
    public static final String TASK_SKIPPED = "workflow.task-skipped";

    private WorkflowEvents() {
    }
}
