package com.datausher.governance.approval.api;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.resource.api.ResourceRef;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ApprovalRequest(
        ApprovalRequestId requestId,
        ApprovalTemplateKey templateKey,
        long templateVersion,
        ApprovalPurpose purpose,
        String title,
        ResourceRef targetResource,
        SubjectRef requestedBy,
        ApprovalRequestStatus status,
        List<ApprovalStep> steps,
        ApprovalCallbackRef callback,
        Instant createdAt,
        Instant completedAt,
        Map<String, String> attributes
) {
    public ApprovalRequest {
        requestId = Objects.requireNonNull(requestId, "requestId must not be null");
        templateKey = Objects.requireNonNull(templateKey, "templateKey must not be null");
        if (templateVersion < 1) {
            throw new IllegalArgumentException("templateVersion must be greater than or equal to 1");
        }
        purpose = Objects.requireNonNull(purpose, "purpose must not be null");
        title = Objects.requireNonNull(title, "title must not be null").trim();
        targetResource = Objects.requireNonNull(targetResource, "targetResource must not be null");
        requestedBy = Objects.requireNonNull(requestedBy, "requestedBy must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        steps = List.copyOf(Objects.requireNonNull(steps, "steps must not be null"));
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (title.isEmpty() || steps.isEmpty()) {
            throw new IllegalArgumentException("title and steps must not be empty");
        }
        if ((status == ApprovalRequestStatus.PENDING) == (completedAt != null)) {
            throw new IllegalArgumentException("completedAt must be null only while pending");
        }
    }
}
