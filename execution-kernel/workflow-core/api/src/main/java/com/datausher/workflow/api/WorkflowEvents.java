package com.datausher.workflow.api;

public final class WorkflowEvents {
    public static final String CREATED = "workflow.created";
    public static final String VERSION_CREATED = "workflow.version-created";
    public static final String PUBLISHED = "workflow.published";
    public static final String TRIGGERED = "workflow.triggered";
    public static final String INSTANCE_STATE_CHANGED = "workflow.instance-state-changed";

    private WorkflowEvents() {
    }
}
