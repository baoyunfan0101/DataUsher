package com.datausher.integration.llm.api;

import com.datausher.integration.runtime.api.AdapterOperation;

public final class LlmOperations {
    public static final AdapterOperation CHAT = AdapterOperation.of(
            "llm.chat.complete", LlmCapabilities.CHAT_COMPLETION, false);
    public static final AdapterOperation EMBED = AdapterOperation.of(
            "llm.embedding.create", LlmCapabilities.EMBEDDING, false);

    private LlmOperations() {
    }
}
