package com.datausher.integration.datasource.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;
import com.datausher.integration.runtime.api.IntegrationValue;

import java.util.HashSet;
import java.util.List;

public record QueryResult(
        List<String> columns,
        List<List<IntegrationValue>> rows,
        boolean truncated
) {
    public QueryResult {
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : rows.stream().map(List::copyOf).toList();
        columns.forEach(column -> IntegrationIdentifiers.requireText(column, "column"));
        if (new HashSet<>(columns).size() != columns.size()) {
            throw new IllegalArgumentException("columns must be unique");
        }
        for (List<IntegrationValue> row : rows) {
            if (row.size() != columns.size()) {
                throw new IllegalArgumentException("row width must match column count");
            }
        }
    }
}
