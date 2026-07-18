package com.datausher.integration.compute.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record ComputeJobLogEntry(
        long sequence,
        Instant occurredAt,
        String level,
        String message,
        Map<String, String> attributes
) {
    public ComputeJobLogEntry {
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must not be negative");
        }
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        level = IntegrationIdentifiers.requireText(level, "level");
        message = Objects.requireNonNull(message, "message must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
