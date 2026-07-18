package com.datausher.execution.api;

import java.util.List;
import java.util.Map;

public record ExecutionResultPage(
        List<ExecutionResultColumn> columns,
        List<List<ExecutionValue>> rows,
        long offset,
        long affectedRows,
        boolean hasMore,
        String resultReference,
        Map<String, String> attributes
) {
    public ExecutionResultPage {
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : rows.stream().map(List::copyOf).toList();
        if (offset < 0 || affectedRows < 0) {
            throw new IllegalArgumentException("offset and affectedRows must not be negative");
        }
        for (List<ExecutionValue> row : rows) {
            if (row.size() != columns.size()) {
                throw new IllegalArgumentException("row width must match column count");
            }
        }
        resultReference = resultReference == null ? "" : resultReference.trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
