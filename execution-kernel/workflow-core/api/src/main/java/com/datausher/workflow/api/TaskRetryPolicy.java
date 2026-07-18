package com.datausher.workflow.api;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

public record TaskRetryPolicy(
        int maxAttempts,
        Duration delay,
        Set<String> retryableFailureCodes
) {
    public static final TaskRetryPolicy NONE = new TaskRetryPolicy(1, Duration.ZERO, Set.of());

    public TaskRetryPolicy {
        delay = Objects.requireNonNull(delay, "delay must not be null");
        retryableFailureCodes = retryableFailureCodes == null
                ? Set.of() : Set.copyOf(retryableFailureCodes);
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be greater than zero");
        }
        if (delay.isNegative()) {
            throw new IllegalArgumentException("delay must not be negative");
        }
    }
}
