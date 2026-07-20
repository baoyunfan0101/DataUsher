package com.datausher.ai.runtime.api;

import com.datausher.integration.llm.api.ChatMessage;
import com.datausher.platform.shared.context.RequestContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AiProviderCallRequest(
        String bindingId,
        String model,
        List<ChatMessage> messages,
        double temperature,
        int maxOutputTokens,
        Instant deadline,
        Map<String, String> options,
        RequestContext requestContext
) {
    public AiProviderCallRequest {
        bindingId = AiRuntimeValues.id(bindingId, "bindingId");
        model = AiRuntimeValues.text(model, "model");
        messages = messages == null ? List.of() : List.copyOf(messages);
        deadline = Objects.requireNonNull(deadline, "deadline must not be null");
        options = AiRuntimeValues.attributes(options);
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
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
