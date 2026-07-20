package com.datausher.ai.runtime.api;

import com.datausher.governance.access.api.SubjectRef;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record AiConversation(
        AiConversationId conversationId,
        String title,
        Optional<SubjectRef> owner,
        AiConversationStatus status,
        Map<String, String> attributes,
        Instant createdAt,
        Instant updatedAt,
        long revision
) {
    public AiConversation {
        conversationId = Objects.requireNonNull(
                conversationId, "conversationId must not be null");
        title = AiRuntimeValues.text(title, "title");
        owner = owner == null ? Optional.empty() : owner;
        status = Objects.requireNonNull(status, "status must not be null");
        attributes = AiRuntimeValues.attributes(attributes);
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (updatedAt.isBefore(createdAt) || revision < 1) {
            throw new IllegalArgumentException("conversation audit fields are invalid");
        }
    }
}
