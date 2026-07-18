package com.datausher.execution.api;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;

public record ExecutionSubmittedEvent(
        String eventId,
        Instant occurredAt,
        RequestContext requestContext,
        ExecutionRequest executionRequest
) implements DomainEvent {
    public ExecutionSubmittedEvent {
        eventId = requireText(eventId, "eventId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        executionRequest = Objects.requireNonNull(executionRequest, "executionRequest must not be null");
    }

    @Override
    public String eventType() {
        return ExecutionEvents.SUBMITTED;
    }

    @Override
    public String sourceModule() {
        return "execution-core";
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
