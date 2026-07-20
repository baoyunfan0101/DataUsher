package com.datausher.ai.context.api;

import java.util.List;
import java.util.Map;

public record AiContextResult(
        List<AiContextItem> items,
        boolean truncated,
        Map<String, String> attributes
) {
    public AiContextResult {
        items = items == null ? List.of() : List.copyOf(items);
        attributes = AiContextValues.attributes(attributes);
    }
}
