package com.datausher.integration.llm.contract;

import com.datausher.integration.llm.api.ChatMessage;
import com.datausher.integration.llm.api.ChatRequest;
import com.datausher.integration.llm.api.ChatResponse;
import com.datausher.integration.llm.api.ChatRole;
import com.datausher.integration.llm.api.LlmCapabilities;
import com.datausher.integration.llm.api.LlmProviderAdapter;
import com.datausher.integration.llm.api.TokenUsage;
import com.datausher.integration.runtime.api.AdapterCapability;
import com.datausher.integration.runtime.api.AdapterDescriptor;
import com.datausher.integration.runtime.api.AdapterHealth;
import com.datausher.integration.runtime.api.AdapterHealthStatus;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.AdapterType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LlmProviderContractTest {
    @Test
    void verifiesChatProviderContract() {
        ChatResponse response = LlmProviderContract.verifyChat(
                new FixtureLlmProvider(), context(), new ChatRequest(
                "default", "fixture-model", List.of(new ChatMessage(
                ChatRole.USER, "hello", "")), 0.1, 50, Map.of()),
                Set.of("secret-token"));

        assertEquals("fixture-model", response.model());
    }

    private static AdapterRequestContext context() {
        return new AdapterRequestContext(
                "request-llm", Instant.EPOCH.plusSeconds(60), Map.of());
    }

    private static final class FixtureLlmProvider implements LlmProviderAdapter {
        @Override
        public ChatResponse chat(AdapterRequestContext context, ChatRequest request) {
            return new ChatResponse(request.model(), new ChatMessage(
                    ChatRole.ASSISTANT, "fixture reply", ""),
                    new TokenUsage(1, 2), "stop", Map.of());
        }

        @Override
        public AdapterDescriptor descriptor() {
            return new AdapterDescriptor("fixture-llm", AdapterType.LLM_PROVIDER, "1",
                    Set.of(AdapterCapability.of(LlmCapabilities.CHAT_COMPLETION)),
                    Map.of());
        }

        @Override
        public AdapterHealth checkHealth() {
            return new AdapterHealth(
                    "fixture-llm", AdapterHealthStatus.UP, Instant.EPOCH, "ok", Map.of());
        }
    }
}
