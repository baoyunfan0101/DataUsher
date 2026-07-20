package com.datausher.ai.guardrails.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AiGuardrailReview(
        boolean allowed,
        AiGuardrailDecisionCode code,
        List<AiGuardrailFinding> findings,
        Map<String, String> attributes,
        Instant reviewedAt
) {
    public AiGuardrailReview {
        code = Objects.requireNonNull(code, "code must not be null");
        findings = findings == null ? List.of() : List.copyOf(findings);
        attributes = AiGuardrailValues.attributes(attributes);
        reviewedAt = Objects.requireNonNull(reviewedAt, "reviewedAt must not be null");
        if (allowed != (code == AiGuardrailDecisionCode.ALLOWED
                || code == AiGuardrailDecisionCode.REDACTED_SENSITIVE_DATA)) {
            throw new IllegalArgumentException("allowed must match guardrail decision code");
        }
    }
}
