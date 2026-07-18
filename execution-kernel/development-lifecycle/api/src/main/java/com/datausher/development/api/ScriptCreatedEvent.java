package com.datausher.development.api;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;

public record ScriptCreatedEvent(
        String eventId,
        Instant occurredAt,
        RequestContext requestContext,
        ScriptDefinition script
) implements DomainEvent {
    public ScriptCreatedEvent {
        eventId = requireText(eventId, "eventId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        script = Objects.requireNonNull(script, "script must not be null");
    }

    @Override
    public String eventType() {
        return DevelopmentEvents.SCRIPT_CREATED;
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
