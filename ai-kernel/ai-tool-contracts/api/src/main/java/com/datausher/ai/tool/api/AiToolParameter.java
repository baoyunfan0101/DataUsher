package com.datausher.ai.tool.api;

import java.util.Map;
import java.util.Objects;

public record AiToolParameter(
        String name,
        AiToolParameterType type,
        boolean required,
        String description,
        Map<String, String> attributes
) {
    public AiToolParameter {
        name = AiToolValues.id(name, "name");
        type = Objects.requireNonNull(type, "type must not be null");
        description = AiToolValues.optionalText(description);
        attributes = AiToolValues.attributes(attributes);
    }
}
