package com.datausher.integration.llm.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.List;
import java.util.Map;

public record EmbeddingRequest(
        String bindingId,
        String model,
        List<String> inputs,
        Map<String, String> options
) {
    public EmbeddingRequest {
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        model = IntegrationIdentifiers.requireText(model, "model");
        inputs = inputs == null ? List.of() : inputs.stream()
                .map(input -> IntegrationIdentifiers.requireText(input, "input"))
                .toList();
        options = options == null ? Map.of() : Map.copyOf(options);
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("inputs must not be empty");
        }
    }
}
