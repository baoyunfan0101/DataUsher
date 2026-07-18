package com.datausher.development.core;

import com.datausher.development.api.DebugRun;
import com.datausher.development.api.DebugRunId;
import com.datausher.development.api.DebugRunService;
import com.datausher.development.api.DebugRunState;
import com.datausher.development.api.DebugRunStateChangedEvent;
import com.datausher.development.api.ScriptQueryService;
import com.datausher.development.api.ScriptVersion;
import com.datausher.development.api.StartDebugRunRequest;
import com.datausher.execution.api.ExecutionCommandService;
import com.datausher.execution.api.ExecutionOrigin;
import com.datausher.execution.api.ExecutionOriginType;
import com.datausher.execution.api.ExecutionRequest;
import com.datausher.execution.api.ExecutionSpecification;
import com.datausher.execution.api.ExecutionWorkload;
import com.datausher.execution.api.SubmitExecutionRequest;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEventPublisher;
import com.datausher.platform.shared.id.IdGenerationRequest;
import com.datausher.platform.shared.id.IdGenerator;
import com.datausher.platform.shared.time.Clock;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DefaultDebugRunService implements DebugRunService, DebugWorker {
    private final ScriptQueryService scripts;
    private final DebugRunStore store;
    private final ExecutionCommandService executions;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final DomainEventPublisher eventPublisher;

    public DefaultDebugRunService(
            ScriptQueryService scripts,
            DebugRunStore store,
            ExecutionCommandService executions,
            IdGenerator idGenerator,
            Clock clock,
            DomainEventPublisher eventPublisher
    ) {
        this.scripts = Objects.requireNonNull(scripts, "scripts must not be null");
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.executions = Objects.requireNonNull(executions, "executions must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    @Override
    public DebugRun start(StartDebugRunRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        requireVersion(request.scriptId(), request.scriptVersion());
        Instant now = clock.now();
        DebugRun proposed = new DebugRun(
                new DebugRunId(idGenerator.nextIdValue(
                        IdGenerationRequest.of("development", "debug-run"))),
                request.scriptId(), request.scriptVersion(), request.idempotencyKey(),
                request.parameters(), DebugRunState.PENDING, Optional.empty(), now, now, 1);
        DebugRunCreateResult result = store.createOrFind(proposed);
        DebugRun existing = result.debugRun();
        if (!result.created()
                && (!existing.scriptId().equals(request.scriptId())
                || existing.scriptVersion() != request.scriptVersion()
                || !existing.parameters().equals(request.parameters()))) {
            throw new IllegalStateException("debug idempotency key was used for a different request");
        }
        if (result.created()) {
            eventPublisher.publish(new DebugRunStateChangedEvent(
                    nextEventId(), now, request.requestContext(), Optional.empty(), existing));
        }
        return existing;
    }

    @Override
    public DebugRun dispatch(DebugRunId debugRunId, RequestContext requestContext) {
        Objects.requireNonNull(debugRunId, "debugRunId must not be null");
        Objects.requireNonNull(requestContext, "requestContext must not be null");
        DebugRun current = store.find(debugRunId)
                .orElseThrow(() -> new IllegalArgumentException("debug run does not exist: " + debugRunId));
        if (current.state() == DebugRunState.SUBMITTED) {
            return current;
        }
        ScriptVersion version = requireVersion(current.scriptId(), current.scriptVersion());
        ExecutionSpecification specification = withParameters(
                version.executionSpecification(), current.parameters());
        ExecutionRequest execution = executions.submit(new SubmitExecutionRequest(
                specification,
                "debug-run:" + current.debugRunId().value(),
                new ExecutionOrigin(
                        ExecutionOriginType.DEBUG_RUN, current.debugRunId().value(), "1",
                        Map.of("scriptId", current.scriptId().value(),
                                "scriptVersion", Long.toString(current.scriptVersion()))),
                requestContext));
        Instant updatedAt = clock.now();
        DebugRunTransitionResult transition = store.markSubmitted(
                current, execution.requestId(), updatedAt);
        if (transition.changed()) {
            eventPublisher.publish(new DebugRunStateChangedEvent(
                    nextEventId(), updatedAt, requestContext,
                    Optional.of(current.state()), transition.debugRun()));
        }
        return transition.debugRun();
    }

    @Override
    public Optional<DebugRun> findDebugRun(DebugRunId debugRunId) {
        return store.find(Objects.requireNonNull(debugRunId, "debugRunId must not be null"));
    }

    private ScriptVersion requireVersion(com.datausher.development.api.ScriptId scriptId, long version) {
        return scripts.findVersion(scriptId, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        "script version does not exist: " + scriptId + "@" + version));
    }

    private static ExecutionSpecification withParameters(
            ExecutionSpecification specification,
            Map<String, com.datausher.execution.api.ExecutionValue> parameters
    ) {
        Map<String, com.datausher.execution.api.ExecutionValue> merged =
                new HashMap<>(specification.workload().parameters());
        merged.putAll(parameters);
        ExecutionWorkload workload = new ExecutionWorkload(
                specification.workload().type(), specification.workload().payload(),
                merged, specification.workload().options());
        return new ExecutionSpecification(
                specification.queueId(), specification.accountId(), workload,
                specification.resultMode(), specification.resultPageSize());
    }

    private String nextEventId() {
        return idGenerator.nextIdValue(IdGenerationRequest.of("development", "domain-event"));
    }
}
