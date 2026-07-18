package com.datausher.workflow.api;

import java.util.Map;
import java.util.Objects;

public record WorkflowRunReference(
        String adapterId,
        String bindingId,
        String externalRunId,
        Map<String, String> attributes
) {
    public WorkflowRunReference {
        adapterId = requireText(adapterId, "adapterId");
        bindingId = requireText(bindingId, "bindingId");
        externalRunId = requireText(externalRunId, "externalRunId");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
