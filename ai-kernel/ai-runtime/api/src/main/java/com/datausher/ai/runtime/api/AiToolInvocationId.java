package com.datausher.ai.runtime.api;

public record AiToolInvocationId(String value) implements Comparable<AiToolInvocationId> {
    public AiToolInvocationId {
        value = AiRuntimeValues.id(value, "value");
    }

    @Override
    public int compareTo(AiToolInvocationId other) {
        return value.compareTo(other.value());
    }
}
