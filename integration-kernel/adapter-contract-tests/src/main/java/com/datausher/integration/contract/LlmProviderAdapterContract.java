package com.datausher.integration.contract;

import com.datausher.integration.llm.api.ChatRequest;
import com.datausher.integration.llm.api.ChatResponse;
import com.datausher.integration.llm.api.EmbeddingProviderAdapter;
import com.datausher.integration.llm.api.EmbeddingRequest;
import com.datausher.integration.llm.api.EmbeddingResponse;
import com.datausher.integration.llm.api.LlmCapabilities;
import com.datausher.integration.llm.api.LlmProviderAdapter;
import com.datausher.integration.runtime.api.AdapterType;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class LlmProviderAdapterContract {
    private LlmProviderAdapterContract() {
    }

    public static void verifyChat(
            LlmProviderAdapter adapter,
            ChatRequest request,
            ChatResponse response,
            Set<String> sensitiveValues
    ) {
        IntegrationAdapterContract.verify(adapter, sensitiveValues);
        assertEquals(AdapterType.LLM_PROVIDER, adapter.descriptor().type());
        assertTrue(adapter.descriptor().supports(LlmCapabilities.CHAT_COMPLETION),
                "chat providers must declare chat completion capability");
        assertNotNull(request);
        assertNotNull(response);
    }

    public static void verifyEmbedding(
            EmbeddingProviderAdapter adapter,
            EmbeddingRequest request,
            EmbeddingResponse response,
            Set<String> sensitiveValues
    ) {
        IntegrationAdapterContract.verify(adapter, sensitiveValues);
        assertEquals(AdapterType.LLM_PROVIDER, adapter.descriptor().type());
        assertTrue(adapter.descriptor().supports(LlmCapabilities.EMBEDDING),
                "embedding providers must declare embedding capability");
        assertEquals(request.inputs().size(), response.vectors().size(),
                "embedding providers must return one vector per input");
    }
}
