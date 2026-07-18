package com.datausher.execution.api;

import java.util.Locale;
import java.util.Objects;

public record ExecutionWorkloadType(String value) implements Comparable<ExecutionWorkloadType> {
    public ExecutionWorkloadType {
        value = Objects.requireNonNull(value, "value must not be null")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z][a-z0-9._-]{0,126}")) {
            throw new IllegalArgumentException(
                    "value must match [a-z][a-z0-9._-]{0,126}");
        }
    }

    @Override
    public int compareTo(ExecutionWorkloadType other) {
        return value.compareTo(Objects.requireNonNull(other, "other must not be null").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
