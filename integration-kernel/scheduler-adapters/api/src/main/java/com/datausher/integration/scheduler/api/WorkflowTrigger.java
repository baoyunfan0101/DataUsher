package com.datausher.integration.scheduler.api;

import com.datausher.integration.runtime.api.IntegrationValue;

import java.util.Map;
import java.util.Objects;

public record WorkflowTrigger(
        PublishedWorkflow workflow,
        String idempotencyKey,
        Map<String, IntegrationValue> parameters
) {
    public WorkflowTrigger {
        workflow = Objects.requireNonNull(workflow, "workflow must not be null");
        idempotencyKey = com.datausher.integration.runtime.api.IntegrationIdentifiers.requireText(
                idempotencyKey, "idempotencyKey");
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
