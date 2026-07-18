package com.datausher.workflow.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record CreateWorkflowVersionRequest(
        WorkflowId workflowId,
        long expectedRevision,
        WorkflowVersionSpec specification,
        RequestContext requestContext
) {
    public CreateWorkflowVersionRequest {
        workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
        specification = Objects.requireNonNull(specification, "specification must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (expectedRevision < 1) {
            throw new IllegalArgumentException("expectedRevision must be greater than zero");
        }
    }
}
