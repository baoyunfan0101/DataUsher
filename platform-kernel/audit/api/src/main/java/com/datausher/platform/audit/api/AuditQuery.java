package com.datausher.platform.audit.api;

import java.time.Instant;

public record AuditQuery(
        String actorId,
        String sourceModule,
        String action,
        AuditTarget target,
        AuditOutcome outcome,
        Instant occurredFrom,
        Instant occurredTo
) {
    public AuditQuery {
        actorId = normalizeNullable(actorId);
        sourceModule = normalizeNullable(sourceModule);
        action = normalizeNullable(action);
        if (occurredFrom != null && occurredTo != null && occurredFrom.isAfter(occurredTo)) {
            throw new IllegalArgumentException("occurredFrom must not be after occurredTo");
        }
    }

    public static AuditQuery all() {
        return new AuditQuery(null, null, null, null, null, null, null);
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
