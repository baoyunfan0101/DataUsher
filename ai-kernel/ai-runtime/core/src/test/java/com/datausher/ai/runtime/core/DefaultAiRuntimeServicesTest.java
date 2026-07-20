package com.datausher.ai.runtime.core;

import com.datausher.ai.runtime.api.AiProviderCallRequest;
import com.datausher.ai.runtime.api.AiRuntimeEvents;
import com.datausher.ai.runtime.api.CompleteAiToolInvocationRequest;
import com.datausher.ai.runtime.api.StartAiConversationRequest;
import com.datausher.ai.runtime.api.StartAiToolInvocationRequest;
import com.datausher.ai.tool.api.AiToolId;
import com.datausher.ai.tool.api.AiToolRef;
import com.datausher.ai.tool.api.AiToolResult;
import com.datausher.ai.tool.api.AiToolResultStatus;
import com.datausher.integration.llm.api.ChatMessage;
import com.datausher.integration.llm.api.ChatRequest;
import com.datausher.integration.llm.api.ChatResponse;
import com.datausher.integration.llm.api.ChatRole;
import com.datausher.integration.llm.api.LlmCapabilities;
import com.datausher.integration.llm.api.LlmProviderAdapter;
import com.datausher.integration.llm.api.TokenUsage;
import com.datausher.integration.runtime.api.AdapterCapability;
import com.datausher.integration.runtime.api.AdapterDescriptor;
import com.datausher.integration.runtime.api.AdapterHealth;
import com.datausher.integration.runtime.api.AdapterHealthStatus;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.AdapterType;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;
import com.datausher.platform.shared.id.GeneratedId;
import com.datausher.platform.shared.id.IdFormat;
import com.datausher.platform.shared.id.IdGenerationRequest;
import com.datausher.platform.shared.id.IdGenerator;
import com.datausher.platform.shared.time.Clock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultAiRuntimeServicesTest {
    @Test
    void conversationsCallProviderThroughLlmAdapter() {
        List<DomainEvent> events = new ArrayList<>();
        DefaultAiConversationService service = new DefaultAiConversationService(
                new InMemoryAiRuntimeStore(), new RecordingLlmProvider(), ids(), clock(),
                events::add);

        var conversation = service.start(new StartAiConversationRequest(
                "Ask data", Optional.empty(), Map.of(), context()));
        var result = service.callProvider(new AiProviderCallRequest(
                "default", "test-model", List.of(new ChatMessage(
                ChatRole.USER, "hello", "")), 0.2, 100,
                Instant.EPOCH.plusSeconds(60), Map.of(), context()));

        assertEquals("conversation-1", conversation.conversationId().value());
        assertEquals("assistant reply", result.response().message().content());
        assertEquals(List.of(AiRuntimeEvents.PROVIDER_CALLED), events.stream()
                .map(DomainEvent::eventType).toList());
    }

    @Test
    void toolInvocationsAreIdempotentAndPublishLifecycleEvents() {
        List<DomainEvent> events = new ArrayList<>();
        DefaultAiToolInvocationService service = new DefaultAiToolInvocationService(
                new InMemoryAiRuntimeStore(), ids(), clock(), events::add);
        StartAiToolInvocationRequest request = new StartAiToolInvocationRequest(
                Optional.empty(), new AiToolRef(new AiToolId("catalog.search"), 1),
                Map.of(), "invoke-1", Map.of(), context());

        var first = service.start(request);
        var duplicate = service.start(request);
        var completed = service.complete(new CompleteAiToolInvocationRequest(
                first.invocationId(), first.revision(), new AiToolResult(
                first.toolRef(), AiToolResultStatus.SUCCEEDED, Map.of(), "ok", Map.of(),
                Instant.EPOCH), context()));

        assertEquals(first.invocationId(), duplicate.invocationId());
        assertEquals(2, completed.revision());
        assertEquals(List.of(
                AiRuntimeEvents.TOOL_INVOCATION_STARTED,
                AiRuntimeEvents.TOOL_INVOCATION_COMPLETED),
                events.stream().map(DomainEvent::eventType).toList());
    }

    private static RequestContext context() {
        return RequestContext.system("request-runtime", Instant.EPOCH);
    }

    private static Clock clock() {
        return new Clock() {
            @Override
            public Instant now() {
                return Instant.EPOCH;
            }

            @Override
            public ZoneId zone() {
                return ZoneId.of("UTC");
            }
        };
    }

    private static IdGenerator ids() {
        AtomicInteger counter = new AtomicInteger();
        return new IdGenerator() {
            @Override
            public GeneratedId nextId(IdGenerationRequest request) {
                return new GeneratedId(
                        Integer.toString(counter.incrementAndGet()),
                        IdFormat.SEQUENCE, request, Map.of());
            }
        };
    }

    private static final class RecordingLlmProvider implements LlmProviderAdapter {
        @Override
        public ChatResponse chat(AdapterRequestContext context, ChatRequest request) {
            return new ChatResponse(request.model(), new ChatMessage(
                    ChatRole.ASSISTANT, "assistant reply", ""),
                    new TokenUsage(1, 2), "stop", Map.of());
        }

        @Override
        public AdapterDescriptor descriptor() {
            return new AdapterDescriptor("test-llm", AdapterType.LLM_PROVIDER, "1",
                    Set.of(AdapterCapability.of(LlmCapabilities.CHAT_COMPLETION)),
                    Map.of());
        }

        @Override
        public AdapterHealth checkHealth() {
            return new AdapterHealth(
                    "test-llm", AdapterHealthStatus.UP, Instant.EPOCH, "ok", Map.of());
        }
    }
}
