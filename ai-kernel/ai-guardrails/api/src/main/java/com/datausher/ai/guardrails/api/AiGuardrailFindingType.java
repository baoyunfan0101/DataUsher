package com.datausher.ai.guardrails.api;

public record AiGuardrailFindingType(String value) {
    public static final AiGuardrailFindingType PERMISSION = new AiGuardrailFindingType("permission");
    public static final AiGuardrailFindingType SQL_SAFETY = new AiGuardrailFindingType("sql-safety");
    public static final AiGuardrailFindingType SENSITIVE_DATA = new AiGuardrailFindingType("sensitive-data");

    public AiGuardrailFindingType {
        value = AiGuardrailValues.id(value, "value");
    }
}
