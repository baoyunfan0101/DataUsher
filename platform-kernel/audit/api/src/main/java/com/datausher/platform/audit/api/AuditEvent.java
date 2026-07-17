package com.datausher.platform.audit.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record AuditEvent(
        String auditId,
        String requestId,
        String actorId,
        String sourceModule,
        String action,
        AuditTarget target,
        AuditOutcome outcome,
        Instant occurredAt,
        Map<String, String> details
) {
    public AuditEvent {
        auditId = normalize(auditId, "auditId");
        requestId = normalize(requestId, "requestId");
        actorId = normalize(actorId, "actorId");
        sourceModule = normalize(sourceModule, "sourceModule");
        action = normalize(action, "action");
        target = Objects.requireNonNull(target, "target must not be null");
        outcome = Objects.requireNonNull(outcome, "outcome must not be null");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
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
