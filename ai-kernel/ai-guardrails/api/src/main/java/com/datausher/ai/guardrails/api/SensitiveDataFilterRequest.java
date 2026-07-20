package com.datausher.ai.guardrails.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record SensitiveDataFilterRequest(
        String content,
        Set<SensitiveDataType> types,
        String replacement,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public SensitiveDataFilterRequest {
        content = AiGuardrailValues.text(content, "content");
        types = types == null ? Set.of() : Set.copyOf(types);
        replacement = replacement == null ? "[redacted]" : replacement;
        attributes = AiGuardrailValues.attributes(attributes);
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
    }
}
