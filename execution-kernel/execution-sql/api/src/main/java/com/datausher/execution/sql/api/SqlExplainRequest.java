package com.datausher.execution.sql.api;

import com.datausher.execution.api.ExecutionAccountId;
import com.datausher.execution.api.ExecutionValue;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;

public record SqlExplainRequest(
        ExecutionAccountId accountId,
        String statement,
        Map<String, ExecutionValue> parameters,
        Map<String, String> options,
        RequestContext requestContext
) {
    public SqlExplainRequest {
        accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        options = options == null ? Map.of() : Map.copyOf(options);
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        statement = SqlWorkloads.statement(statement, parameters, options).payload();
    }
}
