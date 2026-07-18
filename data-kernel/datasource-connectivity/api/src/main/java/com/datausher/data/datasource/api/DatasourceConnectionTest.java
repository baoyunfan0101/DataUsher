package com.datausher.data.datasource.api;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record DatasourceConnectionTest(
        DatasourceId datasourceId,
        boolean successful,
        Duration latency,
        String message,
        Map<String, String> details,
        Instant testedAt
) {
    public DatasourceConnectionTest {
        datasourceId = Objects.requireNonNull(datasourceId, "datasourceId must not be null");
        latency = Objects.requireNonNull(latency, "latency must not be null");
        message = message == null ? "" : message.trim();
        details = details == null ? Map.of() : Map.copyOf(details);
        testedAt = Objects.requireNonNull(testedAt, "testedAt must not be null");
        if (latency.isNegative()) {
            throw new IllegalArgumentException("latency must not be negative");
        }
        if (!successful && message.isEmpty()) {
            throw new IllegalArgumentException("message must describe an unsuccessful test");
        }
    }
}
