package com.datausher.integration.runtime.api;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record AdapterRequestContext(
        String requestId,
        Instant deadline,
        Map<String, String> attributes
) {
    public AdapterRequestContext {
        requestId = IntegrationIdentifiers.requireText(requestId, "requestId");
        deadline = Objects.requireNonNull(deadline, "deadline must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public boolean isExpired(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        return !clock.instant().isBefore(deadline);
    }
}
