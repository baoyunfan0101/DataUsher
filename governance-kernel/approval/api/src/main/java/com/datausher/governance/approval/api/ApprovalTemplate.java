package com.datausher.governance.approval.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ApprovalTemplate(
        ApprovalTemplateKey templateKey,
        long version,
        String displayName,
        ApprovalPurpose purpose,
        List<ApprovalStepDefinition> steps,
        ApprovalTemplateStatus status,
        Instant publishedAt,
        String publishedBy,
        Map<String, String> attributes
) {
    public ApprovalTemplate {
        templateKey = Objects.requireNonNull(templateKey, "templateKey must not be null");
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than or equal to 1");
        }
        displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        purpose = Objects.requireNonNull(purpose, "purpose must not be null");
        steps = List.copyOf(Objects.requireNonNull(steps, "steps must not be null"));
        status = Objects.requireNonNull(status, "status must not be null");
        publishedAt = Objects.requireNonNull(publishedAt, "publishedAt must not be null");
        publishedBy = Objects.requireNonNull(publishedBy, "publishedBy must not be null").trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (displayName.isEmpty() || publishedBy.isEmpty() || steps.isEmpty()) {
            throw new IllegalArgumentException("displayName, publishedBy, and steps must not be empty");
        }
    }
}
