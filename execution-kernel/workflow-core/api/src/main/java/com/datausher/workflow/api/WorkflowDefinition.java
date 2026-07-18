package com.datausher.workflow.api;

import com.datausher.governance.resource.api.ResourceRef;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record WorkflowDefinition(
        WorkflowId workflowId,
        ResourceRef resourceRef,
        String displayName,
        long latestVersion,
        long revision,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        Map<String, String> attributes
) {
    public WorkflowDefinition {
        workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
        resourceRef = Objects.requireNonNull(resourceRef, "resourceRef must not be null");
        displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null").trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (!resourceRef.resourceType().equals("workflow")) {
            throw new IllegalArgumentException("resourceRef must have workflow resource type");
        }
        if (displayName.isEmpty() || createdBy.isEmpty() || latestVersion < 0 || revision < 1) {
            throw new IllegalArgumentException("workflow definition contains invalid values");
        }
    }
}
