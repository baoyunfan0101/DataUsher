package com.datausher.execution.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record ExecutionLogEntry(
        long sequence,
        Instant occurredAt,
        String level,
        String message,
        Map<String, String> attributes
) {
    public ExecutionLogEntry {
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must not be negative");
        }
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        level = Objects.requireNonNull(level, "level must not be null").trim();
        message = Objects.requireNonNull(message, "message must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (level.isEmpty()) {
            throw new IllegalArgumentException("level must not be blank");
        }
    }
}
