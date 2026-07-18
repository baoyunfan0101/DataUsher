package com.datausher.execution.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record SubmitExecutionRequest(
        ExecutionSpecification specification,
        String idempotencyKey,
        ExecutionOrigin origin,
        RequestContext requestContext
) {
    public SubmitExecutionRequest {
        specification = Objects.requireNonNull(specification, "specification must not be null");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null").trim();
        origin = Objects.requireNonNull(origin, "origin must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (idempotencyKey.isEmpty()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
    }
}
