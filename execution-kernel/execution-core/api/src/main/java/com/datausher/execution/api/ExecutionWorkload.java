package com.datausher.execution.api;

import java.util.Map;
import java.util.Objects;

public record ExecutionWorkload(
        ExecutionWorkloadType type,
        String payload,
        Map<String, ExecutionValue> parameters,
        Map<String, String> options
) {
    public ExecutionWorkload {
        type = Objects.requireNonNull(type, "type must not be null");
        payload = Objects.requireNonNull(payload, "payload must not be null").trim();
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        options = options == null ? Map.of() : Map.copyOf(options);
        if (payload.isEmpty()) {
            throw new IllegalArgumentException("payload must not be blank");
        }
        parameters.keySet().forEach(key -> requireKey(key, "parameter"));
        options.keySet().forEach(key -> requireKey(key, "option"));
    }

    private static void requireKey(String key, String kind) {
        if (Objects.requireNonNull(key, kind + " key must not be null").isBlank()) {
            throw new IllegalArgumentException(kind + " key must not be blank");
        }
    }
}
