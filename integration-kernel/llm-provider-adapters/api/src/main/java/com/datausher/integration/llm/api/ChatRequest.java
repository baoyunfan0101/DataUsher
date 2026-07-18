package com.datausher.integration.llm.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.List;
import java.util.Map;

public record ChatRequest(
        String bindingId,
        String model,
        List<ChatMessage> messages,
        double temperature,
        int maxOutputTokens,
        Map<String, String> options
) {
    public ChatRequest {
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        model = IntegrationIdentifiers.requireText(model, "model");
        messages = messages == null ? List.of() : List.copyOf(messages);
        options = options == null ? Map.of() : Map.copyOf(options);
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }
        if (!Double.isFinite(temperature) || temperature < 0 || temperature > 2) {
            throw new IllegalArgumentException("temperature must be between 0 and 2");
        }
        if (maxOutputTokens < 1) {
            throw new IllegalArgumentException("maxOutputTokens must be greater than zero");
        }
    }
}
