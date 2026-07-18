package com.datausher.integration.compute.api;

import com.datausher.integration.runtime.api.IntegrationValue;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ComputeJobResultPage(
        ComputeJobHandle handle,
        List<ComputeResultColumn> columns,
        List<List<IntegrationValue>> rows,
        long offset,
        long affectedRows,
        boolean hasMore,
        String resultReference,
        Map<String, String> attributes
) {
    public ComputeJobResultPage {
        handle = Objects.requireNonNull(handle, "handle must not be null");
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : rows.stream().map(List::copyOf).toList();
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (affectedRows < 0) {
            throw new IllegalArgumentException("affectedRows must not be negative");
        }
        for (List<IntegrationValue> row : rows) {
            if (row.size() != columns.size()) {
                throw new IllegalArgumentException("row width must match column count");
            }
        }
        resultReference = resultReference == null ? "" : resultReference.trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
