package com.datausher.governance.ownership.api;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;

public record ResourceOwnerAssignedEvent(
        String eventId,
        Instant occurredAt,
        RequestContext requestContext,
        ResourceOwner owner
) implements DomainEvent {
    public ResourceOwnerAssignedEvent {
        eventId = normalize(eventId, "eventId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        owner = Objects.requireNonNull(owner, "owner must not be null");
    }

    @Override
    public String eventType() {
        return OwnershipEvents.OWNER_ASSIGNED;
    }

    @Override
    public String sourceModule() {
        return "resource-ownership";
    }

    private static String normalize(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
