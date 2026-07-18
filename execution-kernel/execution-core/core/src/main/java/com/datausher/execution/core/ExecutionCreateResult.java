package com.datausher.execution.core;

import java.util.Objects;

public record ExecutionCreateResult(StoredExecution execution, boolean created) {
    public ExecutionCreateResult {
        execution = Objects.requireNonNull(execution, "execution must not be null");
    }
}
