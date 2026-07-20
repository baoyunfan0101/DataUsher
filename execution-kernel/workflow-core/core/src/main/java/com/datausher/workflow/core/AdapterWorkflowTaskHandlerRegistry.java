package com.datausher.workflow.core;

import com.datausher.integration.runtime.api.AdapterOperation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class AdapterWorkflowTaskHandlerRegistry {
    private final Map<String, AdapterWorkflowTaskHandler> handlers;

    public AdapterWorkflowTaskHandlerRegistry(
            Collection<? extends AdapterWorkflowTaskHandler> handlers
    ) {
        Map<String, AdapterWorkflowTaskHandler> indexed = new HashMap<>();
        for (AdapterWorkflowTaskHandler handler : Objects.requireNonNull(
                handlers, "handlers must not be null")) {
            AdapterWorkflowTaskHandler existing = indexed.putIfAbsent(
                    handler.operation().name(), handler);
            if (existing != null) {
                throw new IllegalArgumentException(
                        "duplicate adapter task handler: " + handler.operation().name());
            }
        }
        this.handlers = Map.copyOf(indexed);
    }

    public AdapterWorkflowTaskHandler require(AdapterOperation operation) {
        Objects.requireNonNull(operation, "operation must not be null");
        AdapterWorkflowTaskHandler handler = handlers.get(operation.name());
        if (handler == null) {
            throw new IllegalStateException(
                    "adapter workflow operation is not supported: " + operation.name());
        }
        return handler;
    }
}
