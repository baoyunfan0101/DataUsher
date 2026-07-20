package com.datausher.ai.guardrails.core;

import com.datausher.ai.guardrails.api.AiGuardrailFinding;
import com.datausher.ai.guardrails.api.SqlSafetyReviewRequest;

import java.util.Optional;

public interface SqlSafetyRule {
    Optional<AiGuardrailFinding> review(SqlSafetyReviewRequest request);
}
