package com.datausher.execution.api;

import java.util.Objects;

public record ExecutionQueueId(String value) implements Comparable<ExecutionQueueId> {
    public ExecutionQueueId {
        value = ExecutionRequestId.normalize(value, "value").toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public int compareTo(ExecutionQueueId other) {
        return value.compareTo(Objects.requireNonNull(other, "other must not be null").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
