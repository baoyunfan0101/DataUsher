package com.datausher.ai.guardrails.api;

public record SensitiveDataType(String value) {
    public static final SensitiveDataType EMAIL = new SensitiveDataType("email");
    public static final SensitiveDataType PHONE = new SensitiveDataType("phone");
    public static final SensitiveDataType SECRET = new SensitiveDataType("secret");

    public SensitiveDataType {
        value = AiGuardrailValues.id(value, "value");
    }
}
