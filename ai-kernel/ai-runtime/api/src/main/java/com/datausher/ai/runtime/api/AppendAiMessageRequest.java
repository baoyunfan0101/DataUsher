package com.datausher.ai.runtime.api;

import com.datausher.integration.llm.api.ChatRole;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record AppendAiMessageRequest(
        AiConversationId conversationId,
        ChatRole role,
        String content,
        Optional<String> name,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public AppendAiMessageRequest {
        conversationId = Objects.requireNonNull(
                conversationId, "conversationId must not be null");
        role = Objects.requireNonNull(role, "role must not be null");
        content = AiRuntimeValues.text(content, "content");
        name = name == null ? Optional.empty() : name.map(String::trim).filter(value -> !value.isEmpty());
        attributes = AiRuntimeValues.attributes(attributes);
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
    }
}
