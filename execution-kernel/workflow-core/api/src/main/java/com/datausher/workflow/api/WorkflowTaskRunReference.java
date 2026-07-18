package com.datausher.workflow.api;

import java.util.Map;
import java.util.Objects;

public record WorkflowTaskRunReference(
        WorkflowTaskRunReferenceType type,
        String referenceId,
        Map<String, String> attributes
) {
    public WorkflowTaskRunReference {
        type = Objects.requireNonNull(type, "type must not be null");
        referenceId = Objects.requireNonNull(referenceId, "referenceId must not be null").trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (referenceId.isEmpty()) {
            throw new IllegalArgumentException("referenceId must not be blank");
        }
    }
}
