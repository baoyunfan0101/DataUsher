package com.datausher.ai.guardrails.api;

import java.util.Map;
import java.util.Objects;

public record AiGuardrailFinding(
        AiGuardrailFindingType type,
        AiGuardrailSeverity severity,
        String message,
        Map<String, String> attributes
) {
    public AiGuardrailFinding {
        type = Objects.requireNonNull(type, "type must not be null");
        severity = Objects.requireNonNull(severity, "severity must not be null");
        message = AiGuardrailValues.text(message, "message");
        attributes = AiGuardrailValues.attributes(attributes);
    }
}
