package com.datausher.development.api;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record DebugRunStateChangedEvent(
        String eventId,
        Instant occurredAt,
        RequestContext requestContext,
        Optional<DebugRunState> previousState,
        DebugRun debugRun
) implements DomainEvent {
    public DebugRunStateChangedEvent {
        eventId = requireText(eventId, "eventId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        previousState = previousState == null ? Optional.empty() : previousState;
        debugRun = Objects.requireNonNull(debugRun, "debugRun must not be null");
        if (previousState.isPresent() && previousState.orElseThrow() == debugRun.state()) {
            throw new IllegalArgumentException("debug run state must change");
        }
    }

    @Override
    public String eventType() {
        return DevelopmentEvents.DEBUG_RUN_STATE_CHANGED;
    }

    @Override
    public String sourceModule() {
        return "development-lifecycle";
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
