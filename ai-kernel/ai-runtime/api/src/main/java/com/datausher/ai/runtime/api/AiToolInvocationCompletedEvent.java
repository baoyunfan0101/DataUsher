package com.datausher.ai.runtime.api;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record AiToolInvocationCompletedEvent(
        String eventId,
        Instant occurredAt,
        RequestContext requestContext,
        AiToolInvocation invocation
) implements DomainEvent {
    public AiToolInvocationCompletedEvent {
        eventId = AiRuntimeValues.text(eventId, "eventId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
        invocation = Objects.requireNonNull(invocation, "invocation must not be null");
    }

    @Override
    public String eventType() {
        return AiRuntimeEvents.TOOL_INVOCATION_COMPLETED;
    }

    @Override
    public String sourceModule() {
        return "ai-runtime";
    }
}
