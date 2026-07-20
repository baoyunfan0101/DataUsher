package com.datausher.ai.context.core;

import com.datausher.ai.context.api.AiContextQuery;
import com.datausher.ai.context.api.AiContextResult;
import com.datausher.ai.context.api.AiContextSourceType;

public interface AiContextProvider {
    AiContextSourceType sourceType();

    AiContextResult search(AiContextQuery query);
}
