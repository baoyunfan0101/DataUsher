package com.datausher.data.quality.api;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;

public record QualityRuleCreatedEvent(
        String eventId,
        Instant occurredAt,
        RequestContext requestContext,
        QualityRule rule,
        QualityRuleVersion initialVersion
) implements DomainEvent {
    public QualityRuleCreatedEvent {
        eventId = QualityValues.text(eventId, "eventId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
        rule = Objects.requireNonNull(rule, "rule must not be null");
        initialVersion = Objects.requireNonNull(
                initialVersion, "initialVersion must not be null");
        if (!rule.ruleId().equals(initialVersion.ruleId()) || initialVersion.version() != 1) {
            throw new IllegalArgumentException("initial rule version must match created rule");
        }
    }

    @Override
    public String eventType() {
        return DataQualityEvents.QUALITY_RULE_CREATED;
    }

    @Override
    public String sourceModule() {
        return "quality-profiler";
    }
}
