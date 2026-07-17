package com.datausher.integration.scheduler.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.Map;

public record WorkflowDefinition(
        String bindingId,
        String workflowId,
        long revision,
        String payload,
        Map<String, String> options
) {
    public WorkflowDefinition {
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        workflowId = IntegrationIdentifiers.normalize(workflowId, "workflowId");
        payload = IntegrationIdentifiers.requireText(payload, "payload");
        options = options == null ? Map.of() : Map.copyOf(options);
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be greater than zero");
        }
    }
}
