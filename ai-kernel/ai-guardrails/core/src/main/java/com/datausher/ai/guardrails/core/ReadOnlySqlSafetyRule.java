package com.datausher.ai.guardrails.core;

import com.datausher.ai.guardrails.api.AiGuardrailFinding;
import com.datausher.ai.guardrails.api.AiGuardrailFindingType;
import com.datausher.ai.guardrails.api.AiGuardrailSeverity;
import com.datausher.ai.guardrails.api.SqlSafetyReviewRequest;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ReadOnlySqlSafetyRule implements SqlSafetyRule {
    private static final Set<String> ALLOWED_PREFIXES = Set.of("select", "with", "explain");

    @Override
    public Optional<AiGuardrailFinding> review(SqlSafetyReviewRequest request) {
        String normalized = request.statement().stripLeading().toLowerCase();
        boolean allowed = ALLOWED_PREFIXES.stream().anyMatch(prefix -> normalized.equals(prefix)
                || normalized.startsWith(prefix + " ") || normalized.startsWith(prefix + "\n"));
        if (allowed) {
            return Optional.empty();
        }
        String firstToken = normalized.split("\\s+", 2)[0];
        return Optional.of(new AiGuardrailFinding(
                AiGuardrailFindingType.SQL_SAFETY, AiGuardrailSeverity.BLOCKING,
                "SQL statement is not read-only", Map.of("firstToken", firstToken)));
    }
}
