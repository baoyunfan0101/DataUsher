package com.datausher.workflow.api;

import java.util.Locale;
import java.util.Objects;

public record WorkflowScheduleType(String value) {
    public static final WorkflowScheduleType CRON = new WorkflowScheduleType("cron");

    public WorkflowScheduleType {
        value = normalize(value);
    }

    private static String normalize(String value) {
        String normalized = Objects.requireNonNull(value, "value must not be null")
                .trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z][a-z0-9.-]{0,126}")) {
            throw new IllegalArgumentException("value must match [a-z][a-z0-9.-]{0,126}");
        }
        return normalized;
    }
}
