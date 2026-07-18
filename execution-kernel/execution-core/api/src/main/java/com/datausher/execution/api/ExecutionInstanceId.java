package com.datausher.execution.api;

import java.util.Objects;

public record ExecutionInstanceId(String value) implements Comparable<ExecutionInstanceId> {
    public ExecutionInstanceId {
        value = ExecutionRequestId.normalize(value, "value");
    }

    @Override
    public int compareTo(ExecutionInstanceId other) {
        return value.compareTo(Objects.requireNonNull(other, "other must not be null").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
