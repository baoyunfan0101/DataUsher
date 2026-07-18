package com.datausher.workflow.api;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;

public record WorkflowVersionCreatedEvent(
        String eventId,
        Instant occurredAt,
        RequestContext requestContext,
        WorkflowVersion workflowVersion
) implements DomainEvent {
    public WorkflowVersionCreatedEvent {
        eventId = requireText(eventId, "eventId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        workflowVersion = Objects.requireNonNull(workflowVersion, "workflowVersion must not be null");
    }

    @Override
    public String eventType() {
        return WorkflowEvents.VERSION_CREATED;
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
