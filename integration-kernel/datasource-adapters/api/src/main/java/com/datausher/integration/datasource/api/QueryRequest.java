package com.datausher.integration.datasource.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;
import com.datausher.integration.runtime.api.IntegrationValue;

import java.util.List;
import java.util.Objects;

public record QueryRequest(
        DatasourceConnection connection,
        String statement,
        List<IntegrationValue> parameters,
        int maxRows
) {
    public QueryRequest {
        connection = Objects.requireNonNull(connection, "connection must not be null");
        statement = IntegrationIdentifiers.requireText(statement, "statement");
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        if (maxRows < 1) {
            throw new IllegalArgumentException("maxRows must be greater than zero");
        }
    }
}
