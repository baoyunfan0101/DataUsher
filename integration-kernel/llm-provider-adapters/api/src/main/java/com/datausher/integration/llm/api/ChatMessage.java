package com.datausher.integration.llm.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.Objects;

public record ChatMessage(ChatRole role, String content, String name) {
    public ChatMessage {
        role = Objects.requireNonNull(role, "role must not be null");
        content = IntegrationIdentifiers.requireText(content, "content");
        name = name == null ? "" : name.trim();
    }
}
