package com.datausher.execution.api;

import java.util.Locale;
import java.util.Objects;

public record ExecutionResultMode(String value) implements Comparable<ExecutionResultMode> {
    public static final ExecutionResultMode PAGED = new ExecutionResultMode("paged");
    public static final ExecutionResultMode REFERENCE = new ExecutionResultMode("reference");
    public static final ExecutionResultMode DISCARD = new ExecutionResultMode("discard");

    public ExecutionResultMode {
        value = Objects.requireNonNull(value, "value must not be null")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z][a-z0-9._-]{0,126}")) {
            throw new IllegalArgumentException(
                    "value must match [a-z][a-z0-9._-]{0,126}");
        }
    }

    @Override
    public int compareTo(ExecutionResultMode other) {
        return value.compareTo(Objects.requireNonNull(other, "other must not be null").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
