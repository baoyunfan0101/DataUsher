package com.datausher.ai.tool.api;

import com.datausher.execution.api.ExecutionValue;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record AiToolResult(
        AiToolRef toolRef,
        AiToolResultStatus status,
        Map<String, ExecutionValue> values,
        String message,
        Map<String, String> attributes,
        Instant completedAt
) {
    public AiToolResult {
        toolRef = Objects.requireNonNull(toolRef, "toolRef must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        values = values == null ? Map.of() : Map.copyOf(values);
        message = AiToolValues.optionalText(message);
        attributes = AiToolValues.attributes(attributes);
        completedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
    }
}
