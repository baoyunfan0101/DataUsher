package com.datausher.integration.scheduler.api;

import com.datausher.integration.runtime.api.AdapterOperation;

public final class SchedulerOperations {
    public static final AdapterOperation PUBLISH_WORKFLOW = AdapterOperation.of(
            "scheduler.workflow.publish", SchedulerCapabilities.WORKFLOW_PUBLICATION, true);
    public static final AdapterOperation UNPUBLISH_WORKFLOW = AdapterOperation.of(
            "scheduler.workflow.unpublish", SchedulerCapabilities.WORKFLOW_PUBLICATION, true);
    public static final AdapterOperation TRIGGER_WORKFLOW = AdapterOperation.of(
            "scheduler.workflow.trigger", SchedulerCapabilities.WORKFLOW_EXECUTION, true);
    public static final AdapterOperation READ_RUN_STATUS = AdapterOperation.of(
            "scheduler.workflow.status", SchedulerCapabilities.WORKFLOW_EXECUTION, false);
    public static final AdapterOperation READ_TASK_RUNS = AdapterOperation.of(
            "scheduler.task.runs.read", SchedulerCapabilities.TASK_OBSERVATION, false);
    public static final AdapterOperation CANCEL_RUN = AdapterOperation.of(
            "scheduler.workflow.cancel", SchedulerCapabilities.WORKFLOW_EXECUTION, true);

    private SchedulerOperations() {
    }
}
