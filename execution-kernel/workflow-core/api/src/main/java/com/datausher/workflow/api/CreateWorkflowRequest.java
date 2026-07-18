package com.datausher.workflow.api;

import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;

public record CreateWorkflowRequest(
        WorkflowId workflowId,
        ResourceRef resourceRef,
        String displayName,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public CreateWorkflowRequest {
        workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
        resourceRef = Objects.requireNonNull(resourceRef, "resourceRef must not be null");
        displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (displayName.isEmpty()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
    }
}
