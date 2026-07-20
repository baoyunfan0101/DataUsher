package com.datausher.ai.context.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AiContextAssemblyRequest(
        List<AiContextItem> items,
        int tokenBudget,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public AiContextAssemblyRequest {
        items = items == null ? List.of() : List.copyOf(items);
        attributes = AiContextValues.attributes(attributes);
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
        if (tokenBudget < 1) {
            throw new IllegalArgumentException("tokenBudget must be greater than zero");
        }
    }
}
