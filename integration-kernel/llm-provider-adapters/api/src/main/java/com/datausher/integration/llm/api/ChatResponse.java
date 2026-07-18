package com.datausher.integration.llm.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.Map;
import java.util.Objects;

public record ChatResponse(
        String model,
        ChatMessage message,
        TokenUsage usage,
        String finishReason,
        Map<String, String> attributes
) {
    public ChatResponse {
        model = IntegrationIdentifiers.requireText(model, "model");
        message = Objects.requireNonNull(message, "message must not be null");
        usage = Objects.requireNonNull(usage, "usage must not be null");
        finishReason = IntegrationIdentifiers.normalize(finishReason, "finishReason");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
