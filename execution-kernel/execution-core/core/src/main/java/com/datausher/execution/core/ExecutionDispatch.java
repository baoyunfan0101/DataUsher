package com.datausher.execution.core;

public record ExecutionDispatch(
        StoredExecution execution,
        StoredExecutionInstance instance
) {
}
