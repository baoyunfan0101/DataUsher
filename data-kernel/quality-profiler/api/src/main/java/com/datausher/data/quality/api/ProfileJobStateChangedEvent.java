package com.datausher.data.quality.api;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record ProfileJobStateChangedEvent(
        String eventId,
        Instant occurredAt,
        RequestContext requestContext,
        Optional<ProfileJobState> previousState,
        ProfileJob job
) implements DomainEvent {
    public ProfileJobStateChangedEvent {
        eventId = QualityValues.text(eventId, "eventId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
        previousState = previousState == null ? Optional.empty() : previousState;
        job = Objects.requireNonNull(job, "job must not be null");
        if (previousState.filter(job.state()::equals).isPresent()) {
            throw new IllegalArgumentException("profile job state must change");
        }
    }

    @Override
    public String eventType() {
        return DataQualityEvents.PROFILE_JOB_STATE_CHANGED;
    }

    @Override
    public String sourceModule() {
        return "quality-profiler";
    }
}
