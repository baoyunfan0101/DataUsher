package com.datausher.data.quality.api;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record DataAnomalyDetectedEvent(
        String eventId,
        Instant occurredAt,
        RequestContext requestContext,
        QualityCheckRun check,
        List<QualityResult> failedResults
) implements DomainEvent {
    public DataAnomalyDetectedEvent {
        eventId = QualityValues.text(eventId, "eventId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
        check = Objects.requireNonNull(check, "check must not be null");
        failedResults = List.copyOf(failedResults);
        QualityCheckId eventCheckId = check.checkId();
        if (failedResults.isEmpty() || failedResults.stream().anyMatch(
                result -> !result.checkId().equals(eventCheckId)
                        || result.outcome() != QualityOutcome.FAILED)) {
            throw new IllegalArgumentException(
                    "anomaly event requires failed results from the same check");
        }
    }

    @Override
    public String eventType() {
        return DataQualityEvents.DATA_ANOMALY_DETECTED;
    }

    @Override
    public String sourceModule() {
        return "quality-profiler";
    }
}
