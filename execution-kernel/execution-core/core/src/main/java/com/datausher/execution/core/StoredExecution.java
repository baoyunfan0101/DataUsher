package com.datausher.execution.core;

import com.datausher.execution.api.ExecutionRequest;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record StoredExecution(ExecutionRequest request, RequestContext requestContext) {
    public StoredExecution {
        request = Objects.requireNonNull(request, "request must not be null");
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
    }
}
