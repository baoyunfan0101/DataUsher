package com.datausher.integration.llm.api;

import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.IntegrationAdapter;

public interface EmbeddingProviderAdapter extends IntegrationAdapter {
    EmbeddingResponse embed(AdapterRequestContext context, EmbeddingRequest request);
}
