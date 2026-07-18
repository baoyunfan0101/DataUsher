package com.datausher.workflow.core;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.workflow.api.WorkflowInstance;
import com.datausher.workflow.api.WorkflowRunReference;

public interface SchedulerManagedWorkflowGateway {
    WorkflowRunReference trigger(WorkflowInstance instance, RequestContext requestContext);

    SchedulerManagedWorkflowObservation observe(
            WorkflowInstance instance,
            RequestContext requestContext
    );

    void cancel(WorkflowInstance instance, RequestContext requestContext);

    static SchedulerManagedWorkflowGateway unsupported() {
        return new SchedulerManagedWorkflowGateway() {
            @Override
            public WorkflowRunReference trigger(
                    WorkflowInstance instance,
                    RequestContext requestContext
            ) {
                throw new IllegalStateException("scheduler-managed workflow runtime is not configured");
            }

            @Override
            public SchedulerManagedWorkflowObservation observe(
                    WorkflowInstance instance,
                    RequestContext requestContext
            ) {
                throw new IllegalStateException("scheduler-managed workflow runtime is not configured");
            }

            @Override
            public void cancel(WorkflowInstance instance, RequestContext requestContext) {
                throw new IllegalStateException("scheduler-managed workflow runtime is not configured");
            }
        };
    }
}
