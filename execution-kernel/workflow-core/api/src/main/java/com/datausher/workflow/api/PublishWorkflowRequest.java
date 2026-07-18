package com.datausher.workflow.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record PublishWorkflowRequest(
        WorkflowId workflowId,
        long version,
        String adapterId,
        String bindingId,
        String idempotencyKey,
        RequestContext requestContext
) {
    public PublishWorkflowRequest {
        workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
        adapterId = requireText(adapterId, "adapterId");
        bindingId = requireText(bindingId, "bindingId");
        idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than zero");
        }
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
