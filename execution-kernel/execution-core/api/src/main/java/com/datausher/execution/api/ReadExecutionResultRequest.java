package com.datausher.execution.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record ReadExecutionResultRequest(
        ExecutionInstanceId instanceId,
        long offset,
        int limit,
        RequestContext requestContext
) {
    public ReadExecutionResultRequest {
        instanceId = Objects.requireNonNull(instanceId, "instanceId must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be greater than zero");
        }
    }
}
