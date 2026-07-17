package com.datausher.platform.shared.id;

import java.util.Map;
import java.util.Objects;

public record GeneratedId(
        String value,
        IdFormat format,
        IdGenerationRequest request,
        Map<String, String> attributes
) {
    public GeneratedId {
        value = Objects.requireNonNull(value, "value must not be null").trim();
        format = Objects.requireNonNull(format, "format must not be null");
        request = Objects.requireNonNull(request, "request must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
