package com.datausher.ai.context.api;

import java.util.List;
import java.util.Map;

public record AiAssembledContext(
        List<AiContextSection> sections,
        int estimatedTokens,
        boolean truncated,
        Map<String, String> attributes
) {
    public AiAssembledContext {
        sections = sections == null ? List.of() : List.copyOf(sections);
        attributes = AiContextValues.attributes(attributes);
        if (estimatedTokens < 0) {
            throw new IllegalArgumentException("estimatedTokens must not be negative");
        }
    }
}
