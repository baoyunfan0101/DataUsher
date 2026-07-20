package com.datausher.ai.tool.api;

public record AiToolId(String value) implements Comparable<AiToolId> {
    public AiToolId {
        value = AiToolValues.id(value, "value");
    }

    @Override
    public int compareTo(AiToolId other) {
        return value.compareTo(other.value());
    }
}
