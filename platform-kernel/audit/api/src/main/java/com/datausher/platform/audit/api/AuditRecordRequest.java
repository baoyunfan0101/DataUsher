package com.datausher.platform.audit.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;

public record AuditRecordRequest(
        RequestContext requestContext,
        String sourceModule,
        String action,
        AuditTarget target,
        AuditOutcome outcome,
        Map<String, String> details
) {
    public AuditRecordRequest {
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        sourceModule = normalize(sourceModule, "sourceModule");
        action = normalize(action, "action");
        target = Objects.requireNonNull(target, "target must not be null");
        outcome = Objects.requireNonNull(outcome, "outcome must not be null");
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    private static String normalize(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
