package com.datausher.execution.core;

import com.datausher.execution.api.ExecutionInstance;
import com.datausher.integration.compute.api.ComputeJobHandle;

import java.util.Objects;
import java.util.Optional;

public record StoredExecutionInstance(
        ExecutionInstance instance,
        Optional<ComputeJobHandle> handle
) {
    public StoredExecutionInstance {
        instance = Objects.requireNonNull(instance, "instance must not be null");
        handle = handle == null ? Optional.empty() : handle;
    }

    public StoredExecutionInstance withHandle(
            ExecutionInstance updated,
            ComputeJobHandle nextHandle
    ) {
        return new StoredExecutionInstance(updated, Optional.of(nextHandle));
    }
}
