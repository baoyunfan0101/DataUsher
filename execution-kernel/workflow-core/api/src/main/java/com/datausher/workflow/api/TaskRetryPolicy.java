package com.datausher.workflow.api;

import java.util.Objects;
import java.util.Set;

public record TaskRetryPolicy(
        int maxAttempts,
        TaskRetryStrategy strategy,
        Set<String> retryableFailureCodes
) {
    public static final TaskRetryPolicy NONE = new TaskRetryPolicy(
            1, TaskRetryStrategy.NONE, Set.of());

    public TaskRetryPolicy {
        strategy = Objects.requireNonNull(strategy, "strategy must not be null");
        retryableFailureCodes = retryableFailureCodes == null
                ? Set.of() : Set.copyOf(retryableFailureCodes);
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be greater than zero");
        }
    }

    public TaskRetryPolicy(
            int maxAttempts,
            java.time.Duration delay,
            Set<String> retryableFailureCodes
    ) {
        this(maxAttempts, TaskRetryStrategy.fixed(delay), retryableFailureCodes);
    }

    public java.time.Duration delay() {
        if (strategy.type().equals(TaskRetryStrategy.NONE_TYPE)) {
            return java.time.Duration.ZERO;
        }
        if (strategy.type().equals(TaskRetryStrategy.FIXED_TYPE)) {
            return java.time.Duration.parse(strategy.options().get("delay"));
        }
        throw new IllegalStateException("retry strategy requires a registered calculator: " + strategy.type());
    }
}
