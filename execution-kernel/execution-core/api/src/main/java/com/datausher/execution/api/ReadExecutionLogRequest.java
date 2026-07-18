package com.datausher.execution.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record ReadExecutionLogRequest(
        ExecutionInstanceId instanceId,
        long afterSequence,
        int limit,
        RequestContext requestContext
) {
    public ReadExecutionLogRequest {
        instanceId = Objects.requireNonNull(instanceId, "instanceId must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (afterSequence < -1) {
            throw new IllegalArgumentException("afterSequence must be at least -1");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be greater than zero");
        }
    }
}
