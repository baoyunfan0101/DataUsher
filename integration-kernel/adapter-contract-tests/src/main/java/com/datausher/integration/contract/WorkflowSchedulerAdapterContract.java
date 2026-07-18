package com.datausher.integration.contract;

import com.datausher.integration.runtime.api.AdapterType;
import com.datausher.integration.scheduler.api.PublishedWorkflow;
import com.datausher.integration.scheduler.api.SchedulerCapabilities;
import com.datausher.integration.scheduler.api.WorkflowDefinition;
import com.datausher.integration.scheduler.api.WorkflowRunHandle;
import com.datausher.integration.scheduler.api.WorkflowSchedulerAdapter;
import com.datausher.integration.scheduler.api.WorkflowTrigger;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class WorkflowSchedulerAdapterContract {
    private WorkflowSchedulerAdapterContract() {
    }

    public static void verifyPublishedWorkflow(
            WorkflowSchedulerAdapter adapter,
            WorkflowDefinition definition,
            PublishedWorkflow workflow,
            Set<String> sensitiveValues
    ) {
        verifyAdapter(adapter, sensitiveValues);
        assertEquals(adapter.descriptor().adapterId(), workflow.adapterId(),
                "published workflows must preserve adapter identity");
        assertEquals(definition.bindingId(), workflow.bindingId(),
                "published workflows must preserve credential binding identity");
        assertEquals(definition.workflowId(), workflow.workflowId(),
                "published workflows must preserve workflow identity");
        assertEquals(definition.revision(), workflow.revision(),
                "published workflows must preserve workflow revision");
    }

    public static void verifyRunHandle(
            WorkflowSchedulerAdapter adapter,
            WorkflowTrigger trigger,
            WorkflowRunHandle handle,
            Set<String> sensitiveValues
    ) {
        verifyAdapter(adapter, sensitiveValues);
        assertEquals(adapter.descriptor().adapterId(), handle.adapterId(),
                "run handles must preserve adapter identity");
        assertEquals(trigger.workflow().bindingId(), handle.bindingId(),
                "run handles must preserve credential binding identity");
        assertEquals(trigger.workflow().workflowId(), handle.workflowId(),
                "run handles must preserve workflow identity");
        assertEquals(trigger.idempotencyKey(), handle.idempotencyKey(),
                "run handles must preserve trigger idempotency identity");
    }

    private static void verifyAdapter(
            WorkflowSchedulerAdapter adapter,
            Set<String> sensitiveValues
    ) {
        IntegrationAdapterContract.verify(adapter, sensitiveValues);
        assertEquals(AdapterType.WORKFLOW_SCHEDULER, adapter.descriptor().type());
        assertTrue(adapter.descriptor().supports(SchedulerCapabilities.WORKFLOW_PUBLICATION),
                "scheduler adapters must declare workflow publication capability");
        assertTrue(adapter.descriptor().supports(SchedulerCapabilities.WORKFLOW_EXECUTION),
                "scheduler adapters must declare workflow execution capability");
        assertTrue(adapter.descriptor().supports(SchedulerCapabilities.TASK_OBSERVATION),
                "scheduler adapters must declare task observation capability");
    }
}
