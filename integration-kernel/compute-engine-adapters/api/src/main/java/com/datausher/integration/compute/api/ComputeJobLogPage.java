package com.datausher.integration.compute.api;

import java.util.List;
import java.util.Objects;

public record ComputeJobLogPage(
        ComputeJobHandle handle,
        List<ComputeJobLogEntry> entries,
        long nextSequence,
        boolean complete
) {
    public ComputeJobLogPage {
        handle = Objects.requireNonNull(handle, "handle must not be null");
        entries = entries == null ? List.of() : List.copyOf(entries);
        if (nextSequence < 0) {
            throw new IllegalArgumentException("nextSequence must not be negative");
        }
        long previous = -1;
        for (ComputeJobLogEntry entry : entries) {
            Objects.requireNonNull(entry, "entries must not contain null");
            if (entry.sequence() <= previous) {
                throw new IllegalArgumentException("entries must be ordered by sequence");
            }
            previous = entry.sequence();
        }
        if (!entries.isEmpty() && nextSequence <= entries.getLast().sequence()) {
            throw new IllegalArgumentException("nextSequence must follow the returned entries");
        }
    }
}
