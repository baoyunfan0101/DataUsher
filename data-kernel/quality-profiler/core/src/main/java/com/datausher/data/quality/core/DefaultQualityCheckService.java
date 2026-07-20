package com.datausher.data.quality.core;

import com.datausher.data.quality.api.CancelQualityCheckRequest;
import com.datausher.data.quality.api.DataAnomalyDetectedEvent;
import com.datausher.data.quality.api.QualityCheckId;
import com.datausher.data.quality.api.QualityCheckRun;
import com.datausher.data.quality.api.QualityCheckService;
import com.datausher.data.quality.api.QualityCheckState;
import com.datausher.data.quality.api.QualityCheckStateChangedEvent;
import com.datausher.data.quality.api.QualityOutcome;
import com.datausher.data.quality.api.QualityResult;
import com.datausher.data.quality.api.QualityRule;
import com.datausher.data.quality.api.QualityRuleQueryService;
import com.datausher.data.quality.api.QualityRuleRef;
import com.datausher.data.quality.api.QualityRuleStatus;
import com.datausher.data.quality.api.QualityRuleVersion;
import com.datausher.data.quality.api.StartQualityCheckRequest;
import com.datausher.execution.api.CancelExecutionRequest;
import com.datausher.execution.api.ExecutionCommandService;
import com.datausher.execution.api.ExecutionOrigin;
import com.datausher.execution.api.ExecutionOriginType;
import com.datausher.execution.api.ExecutionQueryService;
import com.datausher.execution.api.ExecutionRequest;
import com.datausher.execution.api.ExecutionResultMode;
import com.datausher.execution.api.ExecutionSpecification;
import com.datausher.execution.api.ExecutionState;
import com.datausher.execution.api.ExecutionStateChangedEvent;
import com.datausher.execution.api.ExecutionWorkload;
import com.datausher.execution.api.SubmitExecutionRequest;
import com.datausher.platform.shared.concurrent.RevisionConflictException;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEventPublisher;
import com.datausher.platform.shared.id.IdGenerationRequest;
import com.datausher.platform.shared.id.IdGenerator;
import com.datausher.platform.shared.time.Clock;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class DefaultQualityCheckService
        implements QualityCheckService, QualityCheckWorker, QualityExecutionEventHandler {
    private final QualityCheckStore store;
    private final QualityRuleQueryService rules;
    private final ExecutionCommandService executions;
    private final ExecutionQueryService executionQueries;
    private final QualityExecutionPlannerRegistry planners;
    private final QualityResultDecoderRegistry decoders;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final DomainEventPublisher eventPublisher;

    public DefaultQualityCheckService(
            QualityCheckStore store,
            QualityRuleQueryService rules,
            ExecutionCommandService executions,
            ExecutionQueryService executionQueries,
            QualityExecutionPlannerRegistry planners,
            QualityResultDecoderRegistry decoders,
            IdGenerator idGenerator,
            Clock clock,
            DomainEventPublisher eventPublisher
    ) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.rules = Objects.requireNonNull(rules, "rules must not be null");
        this.executions = Objects.requireNonNull(executions, "executions must not be null");
        this.executionQueries = Objects.requireNonNull(
                executionQueries, "executionQueries must not be null");
        this.planners = Objects.requireNonNull(planners, "planners must not be null");
        this.decoders = Objects.requireNonNull(decoders, "decoders must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.eventPublisher = Objects.requireNonNull(
                eventPublisher, "eventPublisher must not be null");
    }

    @Override
    public QualityCheckRun start(StartQualityCheckRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        requireActiveVersions(request.rules());
        Instant now = clock.now();
        QualityCheckRun proposed = new QualityCheckRun(
                new QualityCheckId("check-" + idGenerator.nextIdValue(
                        IdGenerationRequest.of("quality-profiler", "quality-check"))),
                request.rules(), request.executionPolicy(), request.idempotencyKey(),
                QualityCheckState.PENDING, Optional.empty(), Optional.empty(),
                request.attributes(), now, now, Optional.empty(), 1);
        QualityCheckCreateResult result = store.createOrFind(proposed);
        QualityCheckRun existing = result.check();
        if (!result.created() && (!existing.rules().equals(request.rules())
                || !existing.executionPolicy().equals(request.executionPolicy())
                || !existing.attributes().equals(request.attributes()))) {
            throw new IllegalStateException(
                    "quality check idempotency key was used for a different request");
        }
        if (result.created()) {
            publishStateChange(Optional.empty(), existing, request.requestContext());
        }
        return existing;
    }

    @Override
    public List<QualityCheckRun> findPending(int limit) {
        return store.findPending(limit);
    }

    @Override
    public QualityCheckRun dispatch(
            QualityCheckId checkId,
            RequestContext requestContext
    ) {
        Objects.requireNonNull(checkId, "checkId must not be null");
        Objects.requireNonNull(requestContext, "requestContext must not be null");
        QualityCheckRun current = requireCheck(checkId);
        if (current.state() != QualityCheckState.PENDING) {
            return current;
        }
        List<QualityRuleVersion> versions = requireVersions(current.rules());
        ExecutionWorkload workload = Objects.requireNonNull(
                planners.require(current.executionPolicy().executionType())
                        .plan(new QualityCheckPlanningRequest(current, versions)),
                "quality execution planner returned null workload");
        ExecutionSpecification specification = new ExecutionSpecification(
                current.executionPolicy().queueId(), current.executionPolicy().accountId(),
                workload, ExecutionResultMode.REFERENCE,
                current.executionPolicy().resultPageSize());
        ExecutionRequest execution = executions.submit(new SubmitExecutionRequest(
                specification, "quality-check:" + current.checkId().value(),
                new ExecutionOrigin(
                        ExecutionOriginType.QUALITY_CHECK, current.checkId().value(), "1",
                        Map.of("ruleCount", Integer.toString(current.rules().size()))),
                requestContext));
        QualityCheckRun submitted = copy(
                current, QualityCheckState.SUBMITTED, Optional.of(execution.requestId()),
                Optional.empty(), clock.now(), Optional.empty());
        try {
            store.update(current, submitted, List.of());
            publishStateChange(
                    Optional.of(current.state()), submitted, requestContext);
            return submitted;
        } catch (RevisionConflictException conflict) {
            QualityCheckRun concurrent = requireCheck(checkId);
            if (concurrent.executionRequestId().filter(
                    execution.requestId()::equals).isPresent()) {
                return concurrent;
            }
            throw conflict;
        }
    }

    @Override
    public QualityCheckRun cancel(CancelQualityCheckRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        QualityCheckRun current = requireCheck(request.checkId());
        if (current.revision() != request.expectedRevision()) {
            throw new RevisionConflictException(
                    "quality-check", current.checkId().value(),
                    request.expectedRevision(), current.revision());
        }
        if (current.state() == QualityCheckState.CANCELLED) {
            return current;
        }
        if (current.state().terminal()) {
            throw new IllegalStateException("terminal quality check cannot be cancelled");
        }
        if (current.executionRequestId().isPresent()) {
            ExecutionRequest execution = executionQueries.findRequest(
                            current.executionRequestId().orElseThrow())
                    .orElseThrow(() -> new IllegalStateException(
                            "quality check execution does not exist"));
            executions.cancel(new CancelExecutionRequest(
                    execution.requestId(), execution.revision(), request.requestContext()));
        }
        Instant now = clock.now();
        QualityCheckRun cancelled = copy(
                current, QualityCheckState.CANCELLED, current.executionRequestId(),
                Optional.empty(), now, Optional.of(now));
        store.update(current, cancelled, List.of());
        publishStateChange(
                Optional.of(current.state()), cancelled, request.requestContext());
        return cancelled;
    }

    @Override
    public Optional<QualityCheckRun> findCheck(QualityCheckId checkId) {
        return store.find(Objects.requireNonNull(checkId, "checkId must not be null"));
    }

    @Override
    public List<QualityResult> listResults(QualityCheckId checkId) {
        return store.listResults(Objects.requireNonNull(checkId, "checkId must not be null"));
    }

    @Override
    public void handleExecutionStateChanged(ExecutionStateChangedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        ExecutionRequest execution = event.executionRequest();
        if (!execution.origin().type().equals(ExecutionOriginType.QUALITY_CHECK)) {
            return;
        }
        QualityCheckRun current = store.findByExecutionRequest(execution.requestId()).orElse(null);
        if (current == null || current.state().terminal()) {
            return;
        }
        QualityCheckState state = mapState(execution.state());
        if (state == current.state()) {
            return;
        }
        List<QualityResult> results = state == QualityCheckState.SUCCEEDED
                ? decodeAndValidate(current, execution, event.requestContext()) : List.of();
        Optional<String> failureCode = execution.failure().map(value -> value.code());
        Instant now = event.occurredAt();
        QualityCheckRun replacement = copy(
                current, state, current.executionRequestId(), failureCode, now,
                state.terminal() ? Optional.of(now) : Optional.empty());
        store.update(current, replacement, results);
        publishStateChange(
                Optional.of(current.state()), replacement, event.requestContext());
        List<QualityResult> failedResults = results.stream()
                .filter(result -> result.outcome() == QualityOutcome.FAILED).toList();
        if (!failedResults.isEmpty()) {
            eventPublisher.publish(new DataAnomalyDetectedEvent(
                    nextEventId(), now, event.requestContext(), replacement, failedResults));
        }
    }

    private List<QualityResult> decodeAndValidate(
            QualityCheckRun check,
            ExecutionRequest execution,
            RequestContext requestContext
    ) {
        List<QualityRuleVersion> versions = requireVersions(check.rules());
        List<QualityResult> results = List.copyOf(decoders
                .require(execution.specification().workload().type())
                .decode(new QualityResultDecodingRequest(
                        check, versions, execution, requestContext)));
        Map<QualityRuleRef, QualityRuleVersion> expected = versions.stream()
                .collect(Collectors.toUnmodifiableMap(
                        version -> new QualityRuleRef(version.ruleId(), version.version()),
                        version -> version));
        Set<QualityRuleRef> actual = new HashSet<>();
        for (QualityResult result : results) {
            QualityRuleVersion version = expected.get(result.rule());
            if (!result.checkId().equals(check.checkId()) || version == null
                    || !actual.add(result.rule())
                    || !result.severity().equals(version.specification().severity())) {
                throw new IllegalStateException(
                        "quality result contains mismatched or duplicate rules");
            }
        }
        if (!expected.keySet().equals(actual)) {
            throw new IllegalStateException("quality result does not match requested rules");
        }
        return results;
    }

    private void requireActiveVersions(List<QualityRuleRef> references) {
        for (QualityRuleRef reference : references) {
            QualityRule rule = rules.findRule(reference.ruleId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "quality rule does not exist: " + reference.ruleId().value()));
            if (rule.status() != QualityRuleStatus.ACTIVE) {
                throw new IllegalStateException(
                        "quality rule is not active: " + reference.ruleId().value());
            }
            requireVersion(reference);
        }
    }

    private List<QualityRuleVersion> requireVersions(List<QualityRuleRef> references) {
        return references.stream().map(this::requireVersion).toList();
    }

    private QualityRuleVersion requireVersion(QualityRuleRef reference) {
        return rules.findVersion(reference.ruleId(), reference.version())
                .orElseThrow(() -> new IllegalArgumentException(
                        "quality rule version does not exist: "
                                + reference.ruleId().value() + "@" + reference.version()));
    }

    private QualityCheckRun requireCheck(QualityCheckId checkId) {
        return store.find(checkId)
                .orElseThrow(() -> new IllegalArgumentException("quality check does not exist"));
    }

    private static QualityCheckState mapState(ExecutionState state) {
        return switch (state) {
            case PENDING, DISPATCHING, QUEUED -> QualityCheckState.SUBMITTED;
            case RUNNING -> QualityCheckState.RUNNING;
            case SUCCEEDED -> QualityCheckState.SUCCEEDED;
            case FAILED -> QualityCheckState.FAILED;
            case TIMED_OUT -> QualityCheckState.TIMED_OUT;
            case CANCELLED -> QualityCheckState.CANCELLED;
        };
    }

    private static QualityCheckRun copy(
            QualityCheckRun current,
            QualityCheckState state,
            Optional<com.datausher.execution.api.ExecutionRequestId> executionRequestId,
            Optional<String> failureCode,
            Instant now,
            Optional<Instant> finishedAt
    ) {
        return new QualityCheckRun(
                current.checkId(), current.rules(), current.executionPolicy(),
                current.idempotencyKey(), state, executionRequestId, failureCode,
                current.attributes(), current.createdAt(), now, finishedAt,
                current.revision() + 1);
    }

    private void publishStateChange(
            Optional<QualityCheckState> previousState,
            QualityCheckRun check,
            RequestContext requestContext
    ) {
        eventPublisher.publish(new QualityCheckStateChangedEvent(
                nextEventId(), check.updatedAt(), requestContext, previousState, check));
    }

    private String nextEventId() {
        return idGenerator.nextIdValue(
                IdGenerationRequest.of("quality-profiler", "domain-event"));
    }
}
