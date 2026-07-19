package com.datausher.data.quality.api;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record QualityCheckStateChangedEvent(
        String eventId,
        Instant occurredAt,
        RequestContext requestContext,
        Optional<QualityCheckState> previousState,
        QualityCheckRun check
) implements DomainEvent {
    public QualityCheckStateChangedEvent {
        eventId = QualityValues.text(eventId, "eventId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
        previousState = previousState == null ? Optional.empty() : previousState;
        check = Objects.requireNonNull(check, "check must not be null");
        if (previousState.filter(check.state()::equals).isPresent()) {
            throw new IllegalArgumentException("quality check state must change");
        }
    }

    @Override
    public String eventType() {
        return DataQualityEvents.QUALITY_CHECK_STATE_CHANGED;
    }

    @Override
    public String sourceModule() {
        return "quality-profiler";
    }
}
