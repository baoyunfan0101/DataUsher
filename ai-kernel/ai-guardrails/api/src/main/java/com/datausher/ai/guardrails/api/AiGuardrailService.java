package com.datausher.ai.guardrails.api;

public interface AiGuardrailService {
    AiGuardrailReview review(AiGuardrailRequest request);
}
