package com.datausher.ai.guardrails.api;

public interface SqlSafetyReviewService {
    AiGuardrailReview review(SqlSafetyReviewRequest request);
}
