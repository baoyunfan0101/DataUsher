package com.datausher.ai.tool.api;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AiToolSchema(
        AiToolRef ref,
        String displayName,
        String description,
        List<AiToolParameter> parameters,
        AiToolStatus status,
        Map<String, String> attributes,
        long revision
) {
    public AiToolSchema {
        ref = Objects.requireNonNull(ref, "ref must not be null");
        displayName = AiToolValues.text(displayName, "displayName");
        description = AiToolValues.optionalText(description);
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        status = Objects.requireNonNull(status, "status must not be null");
        attributes = AiToolValues.attributes(attributes);
        if (new HashSet<>(parameters.stream().map(AiToolParameter::name).toList()).size()
                != parameters.size()) {
            throw new IllegalArgumentException("tool parameter names must be unique");
        }
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be greater than zero");
        }
    }
}
