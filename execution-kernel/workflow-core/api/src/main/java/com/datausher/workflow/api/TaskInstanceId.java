package com.datausher.workflow.api;

import java.util.Objects;

public record TaskInstanceId(String value) implements Comparable<TaskInstanceId> {
    public TaskInstanceId {
        value = Objects.requireNonNull(value, "value must not be null").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    @Override
    public int compareTo(TaskInstanceId other) {
        return value.compareTo(other.value);
    }
}
