package com.datausher.execution.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;

public record CreateExecutionQueueRequest(
        ExecutionQueueId queueId,
        String displayName,
        int maxConcurrency,
        int priority,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public CreateExecutionQueueRequest {
        queueId = Objects.requireNonNull(queueId, "queueId must not be null");
        displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (displayName.isEmpty()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (maxConcurrency < 1 || priority < 0) {
            throw new IllegalArgumentException(
                    "maxConcurrency must be positive and priority must not be negative");
        }
    }
}
