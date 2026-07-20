package com.datausher.ai.guardrails.api;

import java.util.Map;
import java.util.Objects;

public record SensitiveDataFinding(
        SensitiveDataType type,
        int startInclusive,
        int endExclusive,
        Map<String, String> attributes
) {
    public SensitiveDataFinding {
        type = Objects.requireNonNull(type, "type must not be null");
        attributes = AiGuardrailValues.attributes(attributes);
        if (startInclusive < 0 || endExclusive <= startInclusive) {
            throw new IllegalArgumentException("finding range is invalid");
        }
    }
}
