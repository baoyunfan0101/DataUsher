package com.datausher.development.api;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record ScriptPublicationStateChangedEvent(
        String eventId,
        Instant occurredAt,
        RequestContext requestContext,
        Optional<ScriptPublicationState> previousState,
        ScriptPublication publication
) implements DomainEvent {
    public ScriptPublicationStateChangedEvent {
        eventId = requireText(eventId, "eventId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        previousState = previousState == null ? Optional.empty() : previousState;
        publication = Objects.requireNonNull(publication, "publication must not be null");
        if (previousState.isPresent() && previousState.orElseThrow() == publication.state()) {
            throw new IllegalArgumentException("publication state must change");
        }
    }

    @Override
    public String eventType() {
        return DevelopmentEvents.PUBLICATION_STATE_CHANGED;
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
