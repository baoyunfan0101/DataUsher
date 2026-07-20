package com.datausher.ai.runtime.core;

import com.datausher.ai.runtime.api.AiConversation;
import com.datausher.ai.runtime.api.AiConversationId;
import com.datausher.ai.runtime.api.AiConversationService;
import com.datausher.ai.runtime.api.AiConversationStatus;
import com.datausher.ai.runtime.api.AiMessage;
import com.datausher.ai.runtime.api.AiMessageId;
import com.datausher.ai.runtime.api.AiProviderCallRequest;
import com.datausher.ai.runtime.api.AiProviderCallResult;
import com.datausher.ai.runtime.api.AiProviderCalledEvent;
import com.datausher.ai.runtime.api.AppendAiMessageRequest;
import com.datausher.ai.runtime.api.StartAiConversationRequest;
import com.datausher.integration.llm.api.ChatRequest;
import com.datausher.integration.llm.api.ChatResponse;
import com.datausher.integration.llm.api.LlmProviderAdapter;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.platform.shared.event.DomainEventPublisher;
import com.datausher.platform.shared.id.IdGenerationRequest;
import com.datausher.platform.shared.id.IdGenerator;
import com.datausher.platform.shared.time.Clock;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DefaultAiConversationService implements AiConversationService {
    private final AiRuntimeStore store;
    private final LlmProviderAdapter llmProvider;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final DomainEventPublisher eventPublisher;

    public DefaultAiConversationService(
            AiRuntimeStore store,
            LlmProviderAdapter llmProvider,
            IdGenerator idGenerator,
            Clock clock,
            DomainEventPublisher eventPublisher
    ) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.llmProvider = Objects.requireNonNull(llmProvider, "llmProvider must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.eventPublisher = Objects.requireNonNull(
                eventPublisher, "eventPublisher must not be null");
    }

    @Override
    public AiConversation start(StartAiConversationRequest request) {
        java.time.Instant now = clock.now();
        return store.createConversation(new AiConversation(
                new AiConversationId("conversation-" + idGenerator.nextIdValue(
                        IdGenerationRequest.of("ai-runtime", "conversation"))),
                request.title(), request.owner(), AiConversationStatus.ACTIVE,
                request.attributes(), now, now, 1));
    }

    @Override
    public AiMessage appendMessage(AppendAiMessageRequest request) {
        return store.appendMessage(new AiMessage(
                new AiMessageId("message-" + idGenerator.nextIdValue(
                        IdGenerationRequest.of("ai-runtime", "message"))),
                request.conversationId(), request.role(), request.content(), request.name(),
                request.attributes(), clock.now()));
    }

    @Override
    public Optional<AiConversation> findConversation(AiConversationId conversationId) {
        return store.findConversation(conversationId);
    }

    @Override
    public List<AiMessage> listMessages(AiConversationId conversationId) {
        return store.listMessages(conversationId);
    }

    @Override
    public AiProviderCallResult callProvider(AiProviderCallRequest request) {
        ChatResponse response = llmProvider.chat(new AdapterRequestContext(
                request.requestContext().requestId(), request.deadline(), request.options()),
                new ChatRequest(
                        request.bindingId(), request.model(), request.messages(),
                        request.temperature(), request.maxOutputTokens(), request.options()));
        AiProviderCallResult result = new AiProviderCallResult(
                response, Map.of(), clock.now());
        eventPublisher.publish(new AiProviderCalledEvent(
                nextEventId(), result.completedAt(), request.requestContext(),
                response.model(), response.finishReason()));
        return result;
    }

    private String nextEventId() {
        return "event-" + idGenerator.nextIdValue(
                IdGenerationRequest.of("ai-runtime", "event"));
    }
}
