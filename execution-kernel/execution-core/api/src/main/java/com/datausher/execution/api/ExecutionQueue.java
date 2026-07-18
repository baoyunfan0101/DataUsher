package com.datausher.execution.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record ExecutionQueue(
        ExecutionQueueId queueId,
        String displayName,
        int maxConcurrency,
        int priority,
        ExecutionQueueStatus status,
        Map<String, String> attributes,
        Instant createdAt,
        Instant updatedAt,
        long revision
) {
    public ExecutionQueue {
        queueId = Objects.requireNonNull(queueId, "queueId must not be null");
        displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        status = Objects.requireNonNull(status, "status must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (displayName.isEmpty()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (maxConcurrency < 1) {
            throw new IllegalArgumentException("maxConcurrency must be greater than zero");
        }
        if (priority < 0) {
            throw new IllegalArgumentException("priority must not be negative");
        }
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be greater than zero");
        }
    }

    public ExecutionQueue withStatus(ExecutionQueueStatus nextStatus, Instant changedAt) {
        return new ExecutionQueue(queueId, displayName, maxConcurrency, priority, nextStatus,
                attributes, createdAt, changedAt, revision + 1);
    }
}
