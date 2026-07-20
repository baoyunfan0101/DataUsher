package com.datausher.ai.guardrails.api;

public enum AiGuardrailDecisionCode {
    ALLOWED,
    DENIED_BY_PERMISSION,
    BLOCKED_BY_SQL_SAFETY,
    REDACTED_SENSITIVE_DATA,
    NEEDS_REVIEW
}
