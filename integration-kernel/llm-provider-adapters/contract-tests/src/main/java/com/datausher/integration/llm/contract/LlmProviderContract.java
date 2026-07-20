package com.datausher.integration.llm.contract;

import com.datausher.integration.llm.api.ChatRequest;
import com.datausher.integration.llm.api.ChatResponse;
import com.datausher.integration.llm.api.LlmCapabilities;
import com.datausher.integration.llm.api.LlmProviderAdapter;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.AdapterType;
import com.datausher.integration.runtime.api.SensitiveValueRedactor;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class LlmProviderContract {
    private LlmProviderContract() {
    }

    public static ChatResponse verifyChat(
            LlmProviderAdapter adapter,
            AdapterRequestContext context,
            ChatRequest request,
            Set<String> sensitiveValues
    ) {
        assertNotNull(adapter, "adapter must not be null");
        assertNotNull(context, "context must not be null");
        assertNotNull(request, "request must not be null");
        assertEquals(AdapterType.LLM_PROVIDER, adapter.descriptor().type());
        assertTrue(adapter.descriptor().supports(LlmCapabilities.CHAT_COMPLETION),
                "LLM provider must declare chat completion capability");
        assertEquals(adapter.descriptor().adapterId(), adapter.checkHealth().adapterId(),
                "health must preserve adapter identity");

        ChatResponse response = adapter.chat(context, request);
        assertNotNull(response, "chat response must not be null");
        assertEquals(request.model(), response.model(),
                "provider response must preserve the requested model identity");
        assertNotNull(response.message(), "response message must not be null");
        assertTrue(response.usage().inputTokens() >= 0);
        assertTrue(response.usage().outputTokens() >= 0);
        assertSafe(response.message().content(), response.attributes(), sensitiveValues);
        return response;
    }

    private static void assertSafe(
            String content,
            Map<String, String> attributes,
            Set<String> sensitiveValues
    ) {
        Set<String> checkedValues = sensitiveValues == null ? Set.of() : Set.copyOf(sensitiveValues);
        SensitiveValueRedactor redactor = SensitiveValueRedactor.of(checkedValues);
        assertEquals(content, redactor.redact(content),
                "chat content must not expose sensitive values");
        assertEquals(attributes, redactor.redact(attributes),
                "chat attributes must not expose sensitive values");
    }
}
