package com.datausher.governance.access.api;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record AccessDecision(
        boolean allowed,
        AccessDecisionCode code,
        String reason,
        String matchedPolicyId,
        Instant decidedAt
) {
    public AccessDecision {
        code = Objects.requireNonNull(code, "code must not be null");
        reason = Objects.requireNonNull(reason, "reason must not be null").trim();
        matchedPolicyId = matchedPolicyId == null ? null : matchedPolicyId.trim();
        decidedAt = Objects.requireNonNull(decidedAt, "decidedAt must not be null");
        if (reason.isEmpty()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        if (allowed != (code == AccessDecisionCode.ALLOWED)) {
            throw new IllegalArgumentException("allowed must agree with decision code");
        }
    }

    public Optional<String> matchedPolicy() {
        return Optional.ofNullable(matchedPolicyId);
    }
}
