package com.datausher.integration.llm.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.List;
import java.util.Objects;

public record EmbeddingResponse(
        String model,
        List<List<Double>> vectors,
        TokenUsage usage
) {
    public EmbeddingResponse {
        model = IntegrationIdentifiers.requireText(model, "model");
        vectors = vectors == null ? List.of() : vectors.stream().map(List::copyOf).toList();
        usage = Objects.requireNonNull(usage, "usage must not be null");
        if (vectors.isEmpty()) {
            throw new IllegalArgumentException("vectors must not be empty");
        }
        int dimension = vectors.getFirst().size();
        if (dimension == 0 || vectors.stream().anyMatch(vector -> vector.size() != dimension)) {
            throw new IllegalArgumentException(
                    "embedding vectors must be non-empty and have one dimension");
        }
        if (vectors.stream().flatMap(List::stream).anyMatch(value -> !Double.isFinite(value))) {
            throw new IllegalArgumentException("embedding values must be finite");
        }
    }
}
