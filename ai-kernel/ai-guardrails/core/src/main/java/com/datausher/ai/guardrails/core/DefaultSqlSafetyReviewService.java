package com.datausher.ai.guardrails.core;

import com.datausher.ai.guardrails.api.AiGuardrailDecisionCode;
import com.datausher.ai.guardrails.api.AiGuardrailFinding;
import com.datausher.ai.guardrails.api.AiGuardrailReview;
import com.datausher.ai.guardrails.api.SqlSafetyReviewRequest;
import com.datausher.ai.guardrails.api.SqlSafetyReviewService;
import com.datausher.platform.shared.time.Clock;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DefaultSqlSafetyReviewService implements SqlSafetyReviewService {
    private final List<SqlSafetyRule> rules;
    private final Clock clock;

    public DefaultSqlSafetyReviewService(List<SqlSafetyRule> rules, Clock clock) {
        this.rules = List.copyOf(Objects.requireNonNull(rules, "rules must not be null"));
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public AiGuardrailReview review(SqlSafetyReviewRequest request) {
        List<AiGuardrailFinding> findings = rules.stream()
                .flatMap(rule -> rule.review(request).stream())
                .toList();
        if (findings.isEmpty()) {
            return new AiGuardrailReview(
                    true, AiGuardrailDecisionCode.ALLOWED, List.of(), Map.of(), clock.now());
        }
        return new AiGuardrailReview(false, AiGuardrailDecisionCode.BLOCKED_BY_SQL_SAFETY,
                findings, Map.of(), clock.now());
    }
}
