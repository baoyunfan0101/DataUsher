package com.datausher.workflow.api;

import com.datausher.execution.api.ExecutionValue;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;

public record TriggerWorkflowRequest(
        WorkflowId workflowId,
        long version,
        String idempotencyKey,
        Map<String, ExecutionValue> parameters,
        RequestContext requestContext
) {
    public TriggerWorkflowRequest {
        workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null").trim();
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (version < 1 || idempotencyKey.isEmpty()) {
            throw new IllegalArgumentException("version and idempotencyKey are required");
        }
    }
}
