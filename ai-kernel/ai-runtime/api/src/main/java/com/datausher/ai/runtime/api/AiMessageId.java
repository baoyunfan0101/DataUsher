package com.datausher.ai.runtime.api;

public record AiMessageId(String value) implements Comparable<AiMessageId> {
    public AiMessageId {
        value = AiRuntimeValues.id(value, "value");
    }

    @Override
    public int compareTo(AiMessageId other) {
        return value.compareTo(other.value());
    }
}
