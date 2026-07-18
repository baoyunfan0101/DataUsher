package com.datausher.integration.compute.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record ComputeJobStatus(
        ComputeJobHandle handle,
        ComputeJobState state,
        Instant observedAt,
        String message,
        Map<String, String> details
) {
    public ComputeJobStatus {
        handle = Objects.requireNonNull(handle, "handle must not be null");
        state = Objects.requireNonNull(state, "state must not be null");
        observedAt = Objects.requireNonNull(observedAt, "observedAt must not be null");
        message = message == null ? "" : message.trim();
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    public boolean terminal() {
        return state == ComputeJobState.SUCCEEDED
                || state == ComputeJobState.FAILED
                || state == ComputeJobState.CANCELLED;
    }
}
