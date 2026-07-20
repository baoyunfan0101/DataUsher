package com.datausher.ai.context.api;

import java.util.Map;
import java.util.Objects;

public record AiContextItem(
        AiContextSourceRef source,
        String title,
        String content,
        int priority,
        Map<String, String> attributes
) {
    public AiContextItem {
        source = Objects.requireNonNull(source, "source must not be null");
        title = AiContextValues.text(title, "title");
        content = AiContextValues.text(content, "content");
        attributes = AiContextValues.attributes(attributes);
        if (priority < 0 || priority > 100) {
            throw new IllegalArgumentException("priority must be 0..100");
        }
    }
}
