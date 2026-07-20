package com.datausher.ai.runtime.api;

import com.datausher.integration.llm.api.ChatResponse;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record AiProviderCallResult(
        ChatResponse response,
        Map<String, String> attributes,
        Instant completedAt
) {
    public AiProviderCallResult {
        response = Objects.requireNonNull(response, "response must not be null");
        attributes = AiRuntimeValues.attributes(attributes);
        completedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
    }
}
