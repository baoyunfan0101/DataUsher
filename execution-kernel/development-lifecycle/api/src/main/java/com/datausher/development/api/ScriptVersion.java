package com.datausher.development.api;

import com.datausher.execution.api.ExecutionSpecification;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record ScriptVersion(
        ScriptId scriptId,
        long version,
        ExecutionSpecification executionSpecification,
        Instant createdAt,
        String createdBy,
        Map<String, String> attributes
) {
    public ScriptVersion {
        scriptId = Objects.requireNonNull(scriptId, "scriptId must not be null");
        executionSpecification = Objects.requireNonNull(
                executionSpecification, "executionSpecification must not be null");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null").trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (version < 1 || createdBy.isEmpty()) {
            throw new IllegalArgumentException("version and createdBy are required");
        }
    }
}
