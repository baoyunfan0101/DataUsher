package com.datausher.workflow.api;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;

public record WorkflowTriggeredEvent(
        String eventId,
        Instant occurredAt,
        RequestContext requestContext,
        WorkflowInstance workflowInstance
) implements DomainEvent {
    public WorkflowTriggeredEvent {
        eventId = requireText(eventId, "eventId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        workflowInstance = Objects.requireNonNull(workflowInstance, "workflowInstance must not be null");
    }

    @Override
    public String eventType() {
        return WorkflowEvents.TRIGGERED;
    }

    @Override
    public String sourceModule() {
        return "workflow-core";
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
