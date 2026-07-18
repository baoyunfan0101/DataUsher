package com.datausher.integration.llm.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmContractTest {
    @Test
    void validatesPortableChatSamplingBounds() {
        ChatMessage user = new ChatMessage(ChatRole.USER, "hello", null);

        assertThrows(IllegalArgumentException.class,
                () -> new ChatRequest("primary", "model", List.of(user), 2.1, 100, Map.of()));
        assertEquals(30, new TokenUsage(20, 10).totalTokens());
    }

    @Test
    void requiresConsistentEmbeddingDimensions() {
        assertThrows(IllegalArgumentException.class, () -> new EmbeddingResponse(
                "embed-model",
                List.of(List.of(1.0, 2.0), List.of(3.0)),
                new TokenUsage(2, 0)
        ));
    }

    @Test
    void rejectsEmptyAndNonFiniteEmbeddingVectors() {
        assertThrows(IllegalArgumentException.class, () -> new EmbeddingResponse(
                "embed-model", List.of(), new TokenUsage(0, 0)));
        assertThrows(IllegalArgumentException.class, () -> new EmbeddingResponse(
                "embed-model",
                List.of(List.of(Double.NaN)),
                new TokenUsage(1, 0)
        ));
    }

    @Test
    void publishesCanonicalCapabilitiesForPortableDispatch() {
        assertTrue(LlmCapabilities.CHAT_COMPLETION.startsWith("llm."));
        assertTrue(LlmCapabilities.EMBEDDING.startsWith("llm."));
    }
}
