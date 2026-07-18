package com.datausher.integration.scheduler.api;

import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.IntegrationAdapter;

public interface WorkflowSchedulerAdapter extends IntegrationAdapter {
    PublishedWorkflow publish(AdapterRequestContext context, WorkflowDefinition definition);

    void unpublish(AdapterRequestContext context, PublishedWorkflow workflow);

    WorkflowRunHandle trigger(AdapterRequestContext context, WorkflowTrigger trigger);

    WorkflowRunStatus status(AdapterRequestContext context, WorkflowRunHandle handle);

    WorkflowTaskRunPage readTaskRuns(
            AdapterRequestContext context,
            WorkflowRunHandle handle,
            String cursor,
            int limit
    );

    void cancel(AdapterRequestContext context, WorkflowRunHandle handle);
}
