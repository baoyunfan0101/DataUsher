package com.datausher.ai.runtime.api;

public record AiConversationId(String value) implements Comparable<AiConversationId> {
    public AiConversationId {
        value = AiRuntimeValues.id(value, "value");
    }

    @Override
    public int compareTo(AiConversationId other) {
        return value.compareTo(other.value());
    }
}
