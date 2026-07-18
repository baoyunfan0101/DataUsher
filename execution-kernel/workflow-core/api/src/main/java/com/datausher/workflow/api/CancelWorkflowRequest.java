package com.datausher.workflow.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record CancelWorkflowRequest(
        WorkflowInstanceId instanceId,
        long expectedRevision,
        RequestContext requestContext
) {
    public CancelWorkflowRequest {
        instanceId = Objects.requireNonNull(instanceId, "instanceId must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (expectedRevision < 1) {
            throw new IllegalArgumentException("expectedRevision must be greater than zero");
        }
    }
}
