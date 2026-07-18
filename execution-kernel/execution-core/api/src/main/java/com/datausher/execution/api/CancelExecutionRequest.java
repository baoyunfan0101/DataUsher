package com.datausher.execution.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record CancelExecutionRequest(
        ExecutionRequestId requestId,
        long expectedRevision,
        RequestContext requestContext
) {
    public CancelExecutionRequest {
        requestId = Objects.requireNonNull(requestId, "requestId must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (expectedRevision < 1) {
            throw new IllegalArgumentException("expectedRevision must be greater than zero");
        }
    }
}
