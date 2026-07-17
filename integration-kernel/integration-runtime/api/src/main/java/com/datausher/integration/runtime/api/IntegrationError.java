package com.datausher.integration.runtime.api;

import java.util.Map;
import java.util.Objects;

public record IntegrationError(
        IntegrationErrorCode code,
        String adapterId,
        String message,
        boolean retryable,
        Map<String, String> details
) {
    public IntegrationError {
        code = Objects.requireNonNull(code, "code must not be null");
        adapterId = IntegrationIdentifiers.normalize(adapterId, "adapterId");
        message = IntegrationIdentifiers.requireText(message, "message");
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
