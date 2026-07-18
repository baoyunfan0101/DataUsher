package com.datausher.integration.compute.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;
import com.datausher.integration.runtime.api.IntegrationValue;

import java.util.Map;

public record ComputeJobRequest(
        String bindingId,
        String workloadType,
        String payload,
        Map<String, IntegrationValue> parameters,
        Map<String, String> options
) {
    public ComputeJobRequest {
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        workloadType = IntegrationIdentifiers.normalize(workloadType, "workloadType");
        payload = IntegrationIdentifiers.requireText(payload, "payload");
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        options = options == null ? Map.of() : Map.copyOf(options);
        parameters.keySet().forEach(key ->
                IntegrationIdentifiers.requireText(key, "parameter name"));
        options.keySet().forEach(key ->
                IntegrationIdentifiers.requireText(key, "option name"));
    }
}
