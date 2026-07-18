package com.datausher.governance.approval.api;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;

public record SubmitApprovalRequest(
        ApprovalTemplateKey templateKey,
        String title,
        ResourceRef targetResource,
        SubjectRef requestedBy,
        ApprovalCallbackRef callback,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public SubmitApprovalRequest {
        templateKey = Objects.requireNonNull(templateKey, "templateKey must not be null");
        title = Objects.requireNonNull(title, "title must not be null").trim();
        targetResource = Objects.requireNonNull(targetResource, "targetResource must not be null");
        requestedBy = Objects.requireNonNull(requestedBy, "requestedBy must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (title.isEmpty()) {
            throw new IllegalArgumentException("title must not be blank");
        }
    }
}
