package com.datausher.execution.sql.api;

import java.util.Map;
import java.util.Objects;

public record SqlExplainPlan(
        String format,
        String content,
        Map<String, String> attributes
) {
    public SqlExplainPlan {
        format = Objects.requireNonNull(format, "format must not be null").trim();
        content = Objects.requireNonNull(content, "content must not be null").trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (format.isEmpty() || content.isEmpty()) {
            throw new IllegalArgumentException("format and content must not be blank");
        }
    }
}
