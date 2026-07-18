package com.datausher.integration.compute.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.Map;

public record ComputeJobRequest(
        String bindingId,
        String operation,
        String payload,
        Map<String, String> options
) {
    public ComputeJobRequest {
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        operation = IntegrationIdentifiers.normalize(operation, "operation");
        payload = IntegrationIdentifiers.requireText(payload, "payload");
        options = options == null ? Map.of() : Map.copyOf(options);
    }
}
