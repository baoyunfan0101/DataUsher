package com.datausher.execution.api;

import java.util.Map;
import java.util.Objects;

public record ExecutionResultColumn(
        String name,
        String type,
        boolean nullable,
        Map<String, String> attributes
) {
    public ExecutionResultColumn {
        name = Objects.requireNonNull(name, "name must not be null").trim();
        type = Objects.requireNonNull(type, "type must not be null").trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (name.isEmpty() || type.isEmpty()) {
            throw new IllegalArgumentException("name and type must not be blank");
        }
    }
}
