package com.datausher.integration.scheduler.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulerContractTest {
    @Test
    void requiresPositiveWorkflowRevisions() {
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowDefinition(
                        "scheduler-prod", "daily-load", 0,
                        List.of(new SchedulerTaskDefinition(
                                "extract", SchedulerTaskType.PLATFORM_EXECUTION,
                                "task:extract", Map.of(), Map.of())),
                        List.of(), Optional.empty(), Map.of()));
    }

    @Test
    void exposesStableRunIdentityAndTerminalState() {
        WorkflowRunHandle handle = new WorkflowRunHandle(
                " AIRFLOW ", " SCHEDULER-PROD ", "daily-load", "trigger-1", "run-1");
        WorkflowRunStatus status = new WorkflowRunStatus(
                handle, WorkflowRunState.CANCELLED, Instant.EPOCH, "", Map.of());

        assertEquals("airflow", handle.adapterId());
        assertEquals("scheduler-prod", handle.bindingId());
        assertTrue(status.terminal());
    }

    @Test
    void publishesCanonicalCapabilitiesForPortableDispatch() {
        assertTrue(SchedulerCapabilities.WORKFLOW_PUBLICATION.startsWith("scheduler."));
        assertTrue(SchedulerCapabilities.WORKFLOW_EXECUTION.startsWith("scheduler."));
        assertTrue(SchedulerCapabilities.TASK_OBSERVATION.startsWith("scheduler."));
    }
}
