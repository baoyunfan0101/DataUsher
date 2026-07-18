package com.datausher.execution.api;

import java.util.Objects;

public record ExecutionRequestId(String value) implements Comparable<ExecutionRequestId> {
    public ExecutionRequestId {
        value = normalize(value, "value");
    }

    static String normalize(String value, String name) {
        String normalized = Objects.requireNonNull(value, name + " must not be null").trim();
        if (!normalized.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,126}")) {
            throw new IllegalArgumentException(
                    name + " must match [A-Za-z0-9][A-Za-z0-9._:-]{0,126}");
        }
        return normalized;
    }

    @Override
    public int compareTo(ExecutionRequestId other) {
        return value.compareTo(Objects.requireNonNull(other, "other must not be null").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
