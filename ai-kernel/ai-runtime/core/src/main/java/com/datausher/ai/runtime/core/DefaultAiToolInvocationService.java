package com.datausher.ai.runtime.core;

import com.datausher.ai.runtime.api.AiToolInvocation;
import com.datausher.ai.runtime.api.AiToolInvocationCompletedEvent;
import com.datausher.ai.runtime.api.AiToolInvocationId;
import com.datausher.ai.runtime.api.AiToolInvocationService;
import com.datausher.ai.runtime.api.AiToolInvocationStartedEvent;
import com.datausher.ai.runtime.api.AiToolInvocationStatus;
import com.datausher.ai.runtime.api.CompleteAiToolInvocationRequest;
import com.datausher.ai.runtime.api.StartAiToolInvocationRequest;
import com.datausher.platform.shared.concurrent.RevisionConflictException;
import com.datausher.platform.shared.event.DomainEventPublisher;
import com.datausher.platform.shared.id.IdGenerationRequest;
import com.datausher.platform.shared.id.IdGenerator;
import com.datausher.platform.shared.time.Clock;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class DefaultAiToolInvocationService implements AiToolInvocationService {
    private final AiRuntimeStore store;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final DomainEventPublisher eventPublisher;

    public DefaultAiToolInvocationService(
            AiRuntimeStore store,
            IdGenerator idGenerator,
            Clock clock,
            DomainEventPublisher eventPublisher
    ) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.eventPublisher = Objects.requireNonNull(
                eventPublisher, "eventPublisher must not be null");
    }

    @Override
    public AiToolInvocation start(StartAiToolInvocationRequest request) {
        Instant now = clock.now();
        AiToolInvocation proposed = new AiToolInvocation(
                new AiToolInvocationId("invocation-" + idGenerator.nextIdValue(
                        IdGenerationRequest.of("ai-runtime", "tool-invocation"))),
                request.conversationId(), request.toolRef(), request.arguments(),
                AiToolInvocationStatus.PENDING, Optional.empty(), request.idempotencyKey(),
                request.attributes(), now, now, 1);
        AiToolInvocation invocation = store.createOrFindInvocation(proposed);
        if (invocation.equals(proposed)) {
            eventPublisher.publish(new AiToolInvocationStartedEvent(
                    nextEventId(), now, request.requestContext(), invocation));
        }
        return invocation;
    }

    @Override
    public AiToolInvocation complete(CompleteAiToolInvocationRequest request) {
        AiToolInvocation current = store.findInvocation(request.invocationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "AI tool invocation does not exist"));
        if (current.revision() != request.expectedRevision()) {
            throw new RevisionConflictException(
                    "ai-tool-invocation", current.invocationId().value(),
                    request.expectedRevision(), current.revision());
        }
        if (current.terminal()) {
            return current;
        }
        AiToolInvocationStatus status = switch (request.result().status()) {
            case SUCCEEDED -> AiToolInvocationStatus.SUCCEEDED;
            case DENIED -> AiToolInvocationStatus.DENIED;
            case CANCELLED -> AiToolInvocationStatus.CANCELLED;
            case FAILED -> AiToolInvocationStatus.FAILED;
        };
        AiToolInvocation replacement = new AiToolInvocation(
                current.invocationId(), current.conversationId(), current.toolRef(),
                current.arguments(), status, Optional.of(request.result()),
                current.idempotencyKey(), current.attributes(), current.createdAt(),
                clock.now(), current.revision() + 1);
        AiToolInvocation updated = store.updateInvocation(current, replacement);
        eventPublisher.publish(new AiToolInvocationCompletedEvent(
                nextEventId(), updated.updatedAt(), request.requestContext(), updated));
        return updated;
    }

    @Override
    public Optional<AiToolInvocation> find(AiToolInvocationId invocationId) {
        return store.findInvocation(invocationId);
    }

    private String nextEventId() {
        return "event-" + idGenerator.nextIdValue(
                IdGenerationRequest.of("ai-runtime", "event"));
    }
}
