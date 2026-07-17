package com.datausher.integration.compute.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;
import com.datausher.integration.runtime.api.IntegrationValue;

import java.util.HashSet;
import java.util.List;

public record SqlExecutionResult(
        List<String> columns,
        List<List<IntegrationValue>> rows,
        long affectedRows,
        boolean truncated
) {
    public SqlExecutionResult {
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : rows.stream().map(List::copyOf).toList();
        columns.forEach(column -> IntegrationIdentifiers.requireText(column, "column"));
        if (affectedRows < 0) {
            throw new IllegalArgumentException("affectedRows must not be negative");
        }
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
