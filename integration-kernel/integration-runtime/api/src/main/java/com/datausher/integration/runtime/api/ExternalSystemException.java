package com.datausher.integration.runtime.api;

import java.util.Map;
import java.util.Objects;

public final class ExternalSystemException extends RuntimeException {
    private final IntegrationErrorCode errorCode;
    private final boolean retryable;
    private final Map<String, String> details;

    public ExternalSystemException(
            IntegrationErrorCode errorCode,
            String message,
            boolean retryable
    ) {
        this(errorCode, message, retryable, Map.of(), null);
    }

    public ExternalSystemException(
            IntegrationErrorCode errorCode,
            String message,
            boolean retryable,
            Map<String, String> details,
            Throwable cause
    ) {
        super(IntegrationIdentifiers.requireText(message, "message"), cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.retryable = retryable;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public IntegrationErrorCode errorCode() {
        return errorCode;
    }

    public boolean retryable() {
        return retryable;
    }

    public Map<String, String> details() {
        return details;
    }
}
