package com.datausher.workflow.core;

import com.datausher.workflow.api.WorkflowInstanceId;

import java.time.Instant;
import java.util.Objects;

public record WorkflowRunLease(
        WorkflowInstanceId instanceId,
        String leaseToken,
        String workerId,
        Instant acquiredAt,
        Instant expiresAt
) {
    public WorkflowRunLease {
        instanceId = Objects.requireNonNull(instanceId, "instanceId must not be null");
        leaseToken = requireText(leaseToken, "leaseToken");
        workerId = requireText(workerId, "workerId");
        acquiredAt = Objects.requireNonNull(acquiredAt, "acquiredAt must not be null");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        if (!expiresAt.isAfter(acquiredAt)) {
            throw new IllegalArgumentException("expiresAt must be after acquiredAt");
        }
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(
                value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
