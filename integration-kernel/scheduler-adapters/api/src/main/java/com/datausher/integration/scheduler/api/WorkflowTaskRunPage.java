package com.datausher.integration.scheduler.api;

import java.util.List;
import java.util.Objects;

public record WorkflowTaskRunPage(
        WorkflowRunHandle handle,
        List<WorkflowTaskRunStatus> items,
        String nextCursor,
        boolean complete
) {
    public WorkflowTaskRunPage {
        handle = Objects.requireNonNull(handle, "handle must not be null");
        items = List.copyOf(items);
        nextCursor = nextCursor == null ? "" : nextCursor.trim();
    }
}
