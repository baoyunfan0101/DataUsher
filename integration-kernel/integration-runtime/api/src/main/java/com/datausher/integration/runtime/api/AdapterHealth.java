package com.datausher.integration.runtime.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record AdapterHealth(
        String adapterId,
        AdapterHealthStatus status,
        Instant checkedAt,
        String message,
        Map<String, String> details
) {
    public AdapterHealth {
        adapterId = IntegrationIdentifiers.normalize(adapterId, "adapterId");
        status = Objects.requireNonNull(status, "status must not be null");
        checkedAt = Objects.requireNonNull(checkedAt, "checkedAt must not be null");
        message = message == null ? "" : message.trim();
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
