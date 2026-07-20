package com.datausher.ai.runtime.api;

import com.datausher.integration.llm.api.ChatRole;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record AiMessage(
        AiMessageId messageId,
        AiConversationId conversationId,
        ChatRole role,
        String content,
        Optional<String> name,
        Map<String, String> attributes,
        Instant createdAt
) {
    public AiMessage {
        messageId = Objects.requireNonNull(messageId, "messageId must not be null");
        conversationId = Objects.requireNonNull(
                conversationId, "conversationId must not be null");
        role = Objects.requireNonNull(role, "role must not be null");
        content = AiRuntimeValues.text(content, "content");
        name = name == null ? Optional.empty() : name.map(String::trim).filter(value -> !value.isEmpty());
        attributes = AiRuntimeValues.attributes(attributes);
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
