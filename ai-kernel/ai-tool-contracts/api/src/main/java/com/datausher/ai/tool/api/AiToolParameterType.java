package com.datausher.ai.tool.api;

public record AiToolParameterType(String value) {
    public static final AiToolParameterType STRING = new AiToolParameterType("string");
    public static final AiToolParameterType NUMBER = new AiToolParameterType("number");
    public static final AiToolParameterType BOOLEAN = new AiToolParameterType("boolean");
    public static final AiToolParameterType OBJECT = new AiToolParameterType("object");
    public static final AiToolParameterType ARRAY = new AiToolParameterType("array");

    public AiToolParameterType {
        value = AiToolValues.id(value, "value");
    }
}
