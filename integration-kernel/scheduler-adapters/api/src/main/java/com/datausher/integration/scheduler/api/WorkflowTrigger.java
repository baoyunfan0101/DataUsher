package com.datausher.integration.scheduler.api;

import com.datausher.integration.runtime.api.IntegrationValue;

import java.util.Map;
import java.util.Objects;

public record WorkflowTrigger(
        PublishedWorkflow workflow,
        Map<String, IntegrationValue> parameters
) {
    public WorkflowTrigger {
        workflow = Objects.requireNonNull(workflow, "workflow must not be null");
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
