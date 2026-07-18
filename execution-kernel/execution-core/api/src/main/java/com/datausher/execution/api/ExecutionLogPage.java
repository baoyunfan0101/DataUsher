package com.datausher.execution.api;

import java.util.List;

public record ExecutionLogPage(
        List<ExecutionLogEntry> entries,
        long nextSequence,
        boolean complete
) {
    public ExecutionLogPage {
        entries = entries == null ? List.of() : List.copyOf(entries);
        if (nextSequence < 0) {
            throw new IllegalArgumentException("nextSequence must not be negative");
        }
    }
}
