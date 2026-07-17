package com.datausher.integration.llm.api;

public record TokenUsage(int inputTokens, int outputTokens) {
    public TokenUsage {
        if (inputTokens < 0 || outputTokens < 0) {
            throw new IllegalArgumentException("token counts must not be negative");
        }
    }

    public int totalTokens() {
        return Math.addExact(inputTokens, outputTokens);
    }
}
