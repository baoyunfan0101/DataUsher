package com.datausher.platform.shared.concurrent;

import java.util.Objects;

public final class RevisionConflictException extends IllegalStateException {
    private final String aggregateType;
    private final String aggregateId;
    private final long expectedRevision;
    private final long actualRevision;

    public RevisionConflictException(
            String aggregateType,
            String aggregateId,
            long expectedRevision,
            long actualRevision
    ) {
        super(message(aggregateType, aggregateId, expectedRevision, actualRevision));
        this.aggregateType = requireText(aggregateType, "aggregateType");
        this.aggregateId = requireText(aggregateId, "aggregateId");
        if (expectedRevision < 1 || actualRevision < 1) {
            throw new IllegalArgumentException("revisions must be greater than zero");
        }
        this.expectedRevision = expectedRevision;
        this.actualRevision = actualRevision;
    }

    public String aggregateType() {
        return aggregateType;
    }

    public String aggregateId() {
        return aggregateId;
    }

    public long expectedRevision() {
        return expectedRevision;
    }

    public long actualRevision() {
        return actualRevision;
    }

    private static String message(
            String aggregateType,
            String aggregateId,
            long expectedRevision,
            long actualRevision
    ) {
        return requireText(aggregateType, "aggregateType") + " revision conflict for "
                + requireText(aggregateId, "aggregateId") + ": expected "
                + expectedRevision + " but was " + actualRevision;
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
