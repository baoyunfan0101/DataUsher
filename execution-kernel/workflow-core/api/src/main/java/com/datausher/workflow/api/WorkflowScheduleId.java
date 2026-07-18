package com.datausher.workflow.api;

import java.util.Locale;
import java.util.Objects;

public record WorkflowScheduleId(String value) implements Comparable<WorkflowScheduleId> {
    public WorkflowScheduleId {
        value = Objects.requireNonNull(value, "value must not be null")
                .trim().toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z][a-z0-9.-]{0,126}")) {
            throw new IllegalArgumentException("value contains unsupported characters");
        }
    }

    @Override
    public int compareTo(WorkflowScheduleId other) {
        return value.compareTo(other.value);
    }
}
