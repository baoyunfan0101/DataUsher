package com.datausher.data.quality.api;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;

public record QualityRuleStatusChangedEvent(
        String eventId,
        Instant occurredAt,
        RequestContext requestContext,
        QualityRuleStatus previousStatus,
        QualityRule rule
) implements DomainEvent {
    public QualityRuleStatusChangedEvent {
        eventId = QualityValues.text(eventId, "eventId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
        previousStatus = Objects.requireNonNull(
                previousStatus, "previousStatus must not be null");
        rule = Objects.requireNonNull(rule, "rule must not be null");
        if (previousStatus == rule.status()) {
            throw new IllegalArgumentException("quality rule status must change");
        }
    }

    @Override
    public String eventType() {
        return DataQualityEvents.QUALITY_RULE_STATUS_CHANGED;
    }

    @Override
    public String sourceModule() {
        return "quality-profiler";
    }
}
