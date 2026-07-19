package com.datausher.data.lineage.api;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;

public record LineageUpdatedEvent(
        String eventId,
        Instant occurredAt,
        RequestContext requestContext,
        LineageApplyResult result
) implements DomainEvent {
    public LineageUpdatedEvent {
        eventId = LineageValues.text(eventId, "eventId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
        result = Objects.requireNonNull(result, "result must not be null");
        if (!result.changed()) {
            throw new IllegalArgumentException("lineage updated event requires a changed result");
        }
    }

    @Override
    public String eventType() {
        return LineageEvents.UPDATED;
    }

    @Override
    public String sourceModule() {
        return "lineage";
    }
}
