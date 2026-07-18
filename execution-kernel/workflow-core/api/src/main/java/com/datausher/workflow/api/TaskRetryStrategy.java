package com.datausher.workflow.api;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record TaskRetryStrategy(String type, Map<String, String> options) {
    public static final String NONE_TYPE = "none";
    public static final String FIXED_TYPE = "fixed";
    public static final TaskRetryStrategy NONE = new TaskRetryStrategy(NONE_TYPE, Map.of());

    public TaskRetryStrategy {
        type = Objects.requireNonNull(type, "type must not be null")
                .trim().toLowerCase(Locale.ROOT);
        options = options == null ? Map.of() : Map.copyOf(options);
        if (!type.matches("[a-z][a-z0-9.-]{0,126}")) {
            throw new IllegalArgumentException("type contains unsupported characters");
        }
    }

    public static TaskRetryStrategy fixed(Duration delay) {
        Objects.requireNonNull(delay, "delay must not be null");
        if (delay.isNegative()) {
            throw new IllegalArgumentException("delay must not be negative");
        }
        return new TaskRetryStrategy(FIXED_TYPE, Map.of("delay", delay.toString()));
    }
}
