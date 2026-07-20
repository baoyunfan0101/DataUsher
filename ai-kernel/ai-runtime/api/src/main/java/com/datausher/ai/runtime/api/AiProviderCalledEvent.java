package com.datausher.ai.runtime.api;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record AiProviderCalledEvent(
        String eventId,
        Instant occurredAt,
        RequestContext requestContext,
        String model,
        String finishReason
) implements DomainEvent {
    public AiProviderCalledEvent {
        eventId = AiRuntimeValues.text(eventId, "eventId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
        model = AiRuntimeValues.text(model, "model");
        finishReason = AiRuntimeValues.optionalText(finishReason);
    }

    @Override
    public String eventType() {
        return AiRuntimeEvents.PROVIDER_CALLED;
    }

    @Override
    public String sourceModule() {
        return "ai-runtime";
    }
}
