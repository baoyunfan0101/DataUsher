package com.datausher.integration.compute.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;
import com.datausher.integration.runtime.api.IntegrationValue;

import java.util.Map;

public record SqlExecutionRequest(
        String bindingId,
        String statement,
        Map<String, IntegrationValue> parameters,
        int maxRows
) {
    public SqlExecutionRequest {
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        statement = IntegrationIdentifiers.requireText(statement, "statement");
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        if (maxRows < 1) {
            throw new IllegalArgumentException("maxRows must be greater than zero");
        }
    }
}
