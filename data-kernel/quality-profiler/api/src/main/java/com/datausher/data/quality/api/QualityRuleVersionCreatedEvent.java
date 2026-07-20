package com.datausher.data.quality.api;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;

public record QualityRuleVersionCreatedEvent(
        String eventId,
        Instant occurredAt,
        RequestContext requestContext,
        QualityRuleVersion version
) implements DomainEvent {
    public QualityRuleVersionCreatedEvent {
        eventId = QualityValues.text(eventId, "eventId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
        version = Objects.requireNonNull(version, "version must not be null");
    }

    @Override
    public String eventType() {
        return DataQualityEvents.QUALITY_RULE_VERSION_CREATED;
    }

    @Override
    public String sourceModule() {
        return "quality-profiler";
    }
}
