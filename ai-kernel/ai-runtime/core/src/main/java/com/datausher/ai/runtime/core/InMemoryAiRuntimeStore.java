package com.datausher.ai.runtime.core;

import com.datausher.ai.runtime.api.AiConversation;
import com.datausher.ai.runtime.api.AiConversationId;
import com.datausher.ai.runtime.api.AiMessage;
import com.datausher.ai.runtime.api.AiToolInvocation;
import com.datausher.ai.runtime.api.AiToolInvocationId;
import com.datausher.platform.shared.concurrent.RevisionConflictException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryAiRuntimeStore implements AiRuntimeStore {
    private Map<AiConversationId, AiConversation> conversations = Map.of();
    private Map<AiConversationId, List<AiMessage>> messages = Map.of();
    private Map<AiToolInvocationId, AiToolInvocation> invocations = Map.of();
    private Map<String, AiToolInvocationId> invocationIdempotencyIndex = Map.of();

    @Override
    public synchronized AiConversation createConversation(AiConversation conversation) {
        Map<AiConversationId, AiConversation> next = new LinkedHashMap<>(conversations);
        next.put(conversation.conversationId(), conversation);
        conversations = Map.copyOf(next);
        return conversation;
    }

    @Override
    public synchronized AiMessage appendMessage(AiMessage message) {
        if (!conversations.containsKey(message.conversationId())) {
            throw new IllegalArgumentException("AI conversation does not exist");
        }
        Map<AiConversationId, List<AiMessage>> next = new LinkedHashMap<>(messages);
        List<AiMessage> nextMessages = new ArrayList<>(
                next.getOrDefault(message.conversationId(), List.of()));
        nextMessages.add(message);
        next.put(message.conversationId(), List.copyOf(nextMessages));
        messages = Map.copyOf(next);
        return message;
    }

    @Override
    public synchronized Optional<AiConversation> findConversation(AiConversationId conversationId) {
        return Optional.ofNullable(conversations.get(conversationId));
    }

    @Override
    public synchronized List<AiMessage> listMessages(AiConversationId conversationId) {
        return messages.getOrDefault(conversationId, List.of());
    }

    @Override
    public synchronized AiToolInvocation createOrFindInvocation(AiToolInvocation invocation) {
        AiToolInvocationId existingId = invocationIdempotencyIndex.get(invocation.idempotencyKey());
        if (existingId != null) {
            return invocations.get(existingId);
        }
        Map<AiToolInvocationId, AiToolInvocation> nextInvocations = new LinkedHashMap<>(invocations);
        nextInvocations.put(invocation.invocationId(), invocation);
        invocations = Map.copyOf(nextInvocations);
        Map<String, AiToolInvocationId> nextIndex = new LinkedHashMap<>(invocationIdempotencyIndex);
        nextIndex.put(invocation.idempotencyKey(), invocation.invocationId());
        invocationIdempotencyIndex = Map.copyOf(nextIndex);
        return invocation;
    }

    @Override
    public synchronized AiToolInvocation updateInvocation(
            AiToolInvocation expected,
            AiToolInvocation replacement
    ) {
        AiToolInvocation current = invocations.get(expected.invocationId());
        if (!expected.equals(current)) {
            long currentRevision = current == null ? 0 : current.revision();
            throw new RevisionConflictException(
                    "ai-tool-invocation", expected.invocationId().value(),
                    expected.revision(), currentRevision);
        }
        Map<AiToolInvocationId, AiToolInvocation> next = new LinkedHashMap<>(invocations);
        next.put(replacement.invocationId(), replacement);
        invocations = Map.copyOf(next);
        return replacement;
    }

    @Override
    public synchronized Optional<AiToolInvocation> findInvocation(AiToolInvocationId invocationId) {
        return Optional.ofNullable(invocations.get(invocationId));
    }
}
