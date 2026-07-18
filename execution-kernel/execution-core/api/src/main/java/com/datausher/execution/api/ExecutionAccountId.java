package com.datausher.execution.api;

import java.util.Objects;

public record ExecutionAccountId(String value) implements Comparable<ExecutionAccountId> {
    public ExecutionAccountId {
        value = ExecutionRequestId.normalize(value, "value").toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public int compareTo(ExecutionAccountId other) {
        return value.compareTo(Objects.requireNonNull(other, "other must not be null").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
