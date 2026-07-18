package com.datausher.workflow.api;

import java.time.Instant;
import java.util.Objects;

public record WorkflowVersion(
        WorkflowId workflowId,
        long version,
        WorkflowVersionSpec specification,
        Instant createdAt,
        String createdBy
) {
    public WorkflowVersion {
        workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
        specification = Objects.requireNonNull(specification, "specification must not be null");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null").trim();
        if (version < 1 || createdBy.isEmpty()) {
            throw new IllegalArgumentException("version and createdBy are required");
        }
    }
}
