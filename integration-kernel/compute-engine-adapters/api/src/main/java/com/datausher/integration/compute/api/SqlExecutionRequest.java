package com.datausher.integration.compute.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;
import com.datausher.integration.runtime.api.IntegrationValue;

import java.util.Map;

public record SqlExecutionRequest(
        String bindingId,
        String statement,
        Map<String, IntegrationValue> parameters,
        int maxRows,
        Map<String, String> options
) {
    public SqlExecutionRequest {
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        statement = IntegrationIdentifiers.requireText(statement, "statement");
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        options = options == null ? Map.of() : Map.copyOf(options);
        if (maxRows < 1) {
            throw new IllegalArgumentException("maxRows must be greater than zero");
        }
        parameters.keySet().forEach(key ->
                IntegrationIdentifiers.requireText(key, "parameter name"));
        options.keySet().forEach(key ->
                IntegrationIdentifiers.requireText(key, "option name"));
    }
}
