package com.datausher.integration.datasource.api;

import com.datausher.integration.runtime.api.IntegrationValue;

import java.util.List;
import java.util.Map;

public record StreamBatch(
        List<Map<String, IntegrationValue>> records,
        String nextOffset
) {
    public StreamBatch {
        records = records == null ? List.of() : records.stream().map(Map::copyOf).toList();
        nextOffset = nextOffset == null ? "" : nextOffset.trim();
    }
}
