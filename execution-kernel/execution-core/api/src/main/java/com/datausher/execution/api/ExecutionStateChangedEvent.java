package com.datausher.execution.api;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record ExecutionStateChangedEvent(
        String eventId,
        Instant occurredAt,
        RequestContext requestContext,
        ExecutionRequest executionRequest,
        Optional<ExecutionInstance> executionInstance
) implements DomainEvent {
    public ExecutionStateChangedEvent {
        eventId = requireText(eventId, "eventId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        executionRequest = Objects.requireNonNull(executionRequest, "executionRequest must not be null");
        executionInstance = executionInstance == null ? Optional.empty() : executionInstance;
        if (executionRequest.state() == ExecutionState.PENDING
                || executionRequest.state() == ExecutionState.DISPATCHING) {
            throw new IllegalArgumentException("execution state has no public lifecycle event");
        }
        if (executionInstance.isPresent()) {
            ExecutionInstance instance = executionInstance.orElseThrow();
            if (!instance.requestId().equals(executionRequest.requestId())
                    || instance.state() != executionRequest.state()) {
                throw new IllegalArgumentException("execution instance must match request state and identity");
            }
        }
    }

    @Override
    public String eventType() {
        return switch (executionRequest.state()) {
            case QUEUED -> ExecutionEvents.QUEUED;
            case RUNNING -> ExecutionEvents.STARTED;
            case SUCCEEDED -> ExecutionEvents.COMPLETED;
            case FAILED -> ExecutionEvents.FAILED;
            case CANCELLED -> ExecutionEvents.CANCELLED;
            case TIMED_OUT -> ExecutionEvents.TIMED_OUT;
            case PENDING, DISPATCHING -> throw new IllegalStateException(
                    "execution state has no public lifecycle event");
        };
    }

    @Override
    public String sourceModule() {
        return "execution-core";
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
