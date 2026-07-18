package com.datausher.integration.llm.api;

import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.IntegrationAdapter;

public interface LlmProviderAdapter extends IntegrationAdapter {
    ChatResponse chat(AdapterRequestContext context, ChatRequest request);
}
