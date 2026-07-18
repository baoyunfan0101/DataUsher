package com.datausher.workflow.api;

import java.time.Instant;
import java.util.Objects;

public record WorkflowPublication(
        WorkflowId workflowId,
        long version,
        String adapterId,
        String bindingId,
        String idempotencyKey,
        String externalWorkflowId,
        long externalRevision,
        Instant publishedAt,
        String publishedBy
) {
    public WorkflowPublication {
        workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
        adapterId = requireText(adapterId, "adapterId");
        bindingId = requireText(bindingId, "bindingId");
        idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
        externalWorkflowId = requireText(externalWorkflowId, "externalWorkflowId");
        publishedAt = Objects.requireNonNull(publishedAt, "publishedAt must not be null");
        publishedBy = requireText(publishedBy, "publishedBy");
        if (version < 1 || externalRevision < 1) {
            throw new IllegalArgumentException("versions must be greater than zero");
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
