package com.datausher.platform.shared.event;

import com.datausher.platform.shared.context.RequestContext;

import java.time.Instant;
import java.util.Objects;

public record BaseDomainEvent(
        String eventId,
        String eventType,
        String sourceModule,
        Instant occurredAt,
        RequestContext requestContext
) implements DomainEvent {
    public BaseDomainEvent {
        eventId = Objects.requireNonNull(eventId, "eventId must not be null").trim();
        eventType = Objects.requireNonNull(eventType, "eventType must not be null").trim();
        sourceModule = Objects.requireNonNull(sourceModule, "sourceModule must not be null").trim();
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (eventId.isEmpty()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        if (eventType.isEmpty()) {
            throw new IllegalArgumentException("eventType must not be blank");
        }
        if (sourceModule.isEmpty()) {
            throw new IllegalArgumentException("sourceModule must not be blank");
        }
    }
}
