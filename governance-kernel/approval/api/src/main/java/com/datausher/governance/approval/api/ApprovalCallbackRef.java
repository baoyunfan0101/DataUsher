package com.datausher.governance.approval.api;

import java.util.Map;
import java.util.Objects;

public record ApprovalCallbackRef(
        ApprovalCallbackType type,
        String correlationKey,
        Map<String, String> parameters
) {
    public ApprovalCallbackRef {
        type = Objects.requireNonNull(type, "type must not be null");
        correlationKey = Objects.requireNonNull(correlationKey, "correlationKey must not be null").trim();
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        if (correlationKey.isEmpty()) {
            throw new IllegalArgumentException("correlationKey must not be blank");
        }
    }
}
