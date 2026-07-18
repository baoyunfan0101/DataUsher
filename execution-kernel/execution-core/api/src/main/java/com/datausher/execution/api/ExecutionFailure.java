package com.datausher.execution.api;

import java.util.Map;
import java.util.Objects;

public record ExecutionFailure(
        String code,
        String message,
        boolean retryable,
        Map<String, String> details
) {
    public ExecutionFailure {
        code = Objects.requireNonNull(code, "code must not be null").trim();
        message = Objects.requireNonNull(message, "message must not be null").trim();
        details = details == null ? Map.of() : Map.copyOf(details);
        if (code.isEmpty()) {
            throw new IllegalArgumentException("code must not be blank");
        }
    }
}
