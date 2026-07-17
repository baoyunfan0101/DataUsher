package com.datausher.integration.datasource.api;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public record ConnectionTestResult(
        boolean successful,
        Duration latency,
        String message,
        Map<String, String> details
) {
    public ConnectionTestResult {
        latency = Objects.requireNonNull(latency, "latency must not be null");
        message = message == null ? "" : message.trim();
        details = details == null ? Map.of() : Map.copyOf(details);
        if (latency.isNegative()) {
            throw new IllegalArgumentException("latency must not be negative");
        }
        if (!successful && message.isEmpty()) {
            throw new IllegalArgumentException("message must describe an unsuccessful test");
        }
    }
}
