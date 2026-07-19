package com.datausher.data.quality.core;

import com.datausher.data.quality.api.CancelProfileJobRequest;
import com.datausher.data.quality.api.ProfileCommandService;
import com.datausher.data.quality.api.ProfileJob;
import com.datausher.data.quality.api.ProfileJobId;
import com.datausher.data.quality.api.ProfileJobState;
import com.datausher.data.quality.api.ProfileMetric;
import com.datausher.data.quality.api.ProfileJobStateChangedEvent;
import com.datausher.data.quality.api.ProfileQueryService;
import com.datausher.data.quality.api.StartProfileJobRequest;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class DefaultProfileService
        implements ProfileCommandService, ProfileQueryService, ProfileWorker,
        ProfileExecutionEventHandler {
    private final ProfileStore store;
    private final ExecutionCommandService executions;
    private final ExecutionQueryService executionQueries;
    private final ProfileExecutionPlannerRegistry planners;
    private final ProfileResultDecoderRegistry decoders;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final DomainEventPublisher eventPublisher;

    public DefaultProfileService(
            ProfileStore store,
            ExecutionCommandService executions,
            ExecutionQueryService executionQueries,
            ProfileExecutionPlannerRegistry planners,
            ProfileResultDecoderRegistry decoders,
            IdGenerator idGenerator,
            Clock clock,
            DomainEventPublisher eventPublisher
    ) {
        this.store = Objects.requireNonNull(store, "store must not be null");
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
    public ProfileJob start(StartProfileJobRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Instant now = clock.now();
        ProfileJob proposed = new ProfileJob(
                new ProfileJobId("profile-" + idGenerator.nextIdValue(
                        IdGenerationRequest.of("quality-profiler", "profile-job"))),
                request.target(), request.metrics(), request.executionPolicy(),
                request.idempotencyKey(), ProfileJobState.PENDING,
                Optional.empty(), Optional.empty(), request.attributes(),
                now, now, Optional.empty(), 1);
        ProfileJobCreateResult result = store.createOrFind(proposed);
        ProfileJob existing = result.job();
        if (!result.created() && (!existing.target().equals(request.target())
                || !existing.metrics().equals(request.metrics())
                || !existing.executionPolicy().equals(request.executionPolicy())
                || !existing.attributes().equals(request.attributes()))) {
            throw new IllegalStateException(
                    "profile idempotency key was used for a different request");
        }
        if (result.created()) {
            publishStateChange(Optional.empty(), existing, request.requestContext());
        }
        return existing;
    }

    @Override
    public List<ProfileJob> findPending(int limit) {
        return store.findPending(limit);
    }

    @Override
    public ProfileJob dispatch(ProfileJobId jobId, RequestContext requestContext) {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(requestContext, "requestContext must not be null");
        ProfileJob current = requireJob(jobId);
        if (current.state() != ProfileJobState.PENDING) {
            return current;
        }
        ExecutionWorkload workload = Objects.requireNonNull(
                planners.require(current.executionPolicy().executionType()).plan(current),
                "profile execution planner returned null workload");
        ExecutionSpecification specification = new ExecutionSpecification(
                current.executionPolicy().queueId(), current.executionPolicy().accountId(),
                workload, ExecutionResultMode.REFERENCE,
                current.executionPolicy().resultPageSize());
        ExecutionRequest execution = executions.submit(new SubmitExecutionRequest(
                specification, "profile-job:" + current.jobId().value(),
                new ExecutionOrigin(
                        ExecutionOriginType.PROFILE_JOB, current.jobId().value(), "1",
                        Map.of("targetType", current.target().type().value(),
                                "targetId", current.target().targetId())),
                requestContext));
        ProfileJob submitted = copy(
                current, ProfileJobState.SUBMITTED, Optional.of(execution.requestId()),
                Optional.empty(), clock.now(), Optional.empty());
        try {
            store.update(current, submitted, List.of());
            publishStateChange(
                    Optional.of(current.state()), submitted, requestContext);
            return submitted;
        } catch (RevisionConflictException conflict) {
            ProfileJob concurrent = requireJob(jobId);
            if (concurrent.executionRequestId().filter(
                    execution.requestId()::equals).isPresent()) {
                return concurrent;
            }
            throw conflict;
        }
    }

    @Override
    public ProfileJob cancel(CancelProfileJobRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        ProfileJob current = requireJob(request.jobId());
        if (current.revision() != request.expectedRevision()) {
            throw new RevisionConflictException(
                    "profile-job", current.jobId().value(),
                    request.expectedRevision(), current.revision());
        }
        if (current.state() == ProfileJobState.CANCELLED) {
            return current;
        }
        if (current.state().terminal()) {
            throw new IllegalStateException("terminal profile job cannot be cancelled");
        }
        if (current.executionRequestId().isEmpty()) {
            ProfileJob cancelled = copy(
                    current, ProfileJobState.CANCELLED, Optional.empty(), Optional.empty(),
                    clock.now(), Optional.of(clock.now()));
            store.update(current, cancelled, List.of());
            publishStateChange(
                    Optional.of(current.state()), cancelled, request.requestContext());
            return cancelled;
        }
        ExecutionRequest execution = executionQueries.findRequest(
                        current.executionRequestId().orElseThrow())
                .orElseThrow(() -> new IllegalStateException("profile execution does not exist"));
        executions.cancel(new CancelExecutionRequest(
                execution.requestId(), execution.revision(), request.requestContext()));
        Instant now = clock.now();
        ProfileJob cancelled = copy(
                current, ProfileJobState.CANCELLED, current.executionRequestId(),
                Optional.empty(), now, Optional.of(now));
        store.update(current, cancelled, List.of());
        publishStateChange(
                Optional.of(current.state()), cancelled, request.requestContext());
        return cancelled;
    }

    @Override
    public Optional<ProfileJob> findJob(ProfileJobId jobId) {
        return store.find(Objects.requireNonNull(jobId, "jobId must not be null"));
    }

    @Override
    public List<ProfileMetric> listMetrics(ProfileJobId jobId) {
        return store.listMetrics(Objects.requireNonNull(jobId, "jobId must not be null"));
    }

    @Override
    public void handleExecutionStateChanged(ExecutionStateChangedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        ExecutionRequest execution = event.executionRequest();
        if (!execution.origin().type().equals(ExecutionOriginType.PROFILE_JOB)) {
            return;
        }
        ProfileJob current = store.findByExecutionRequest(execution.requestId()).orElse(null);
        if (current == null || current.state().terminal()) {
            return;
        }
        ProfileJobState state = mapState(execution.state());
        if (state == current.state()) {
            return;
        }
        List<ProfileMetric> metrics = state == ProfileJobState.SUCCEEDED
                ? decodeAndValidate(current, execution, event.requestContext()) : List.of();
        Optional<String> failureCode = execution.failure().map(value -> value.code());
        Instant now = event.occurredAt();
        ProfileJob replacement = copy(
                current, state, current.executionRequestId(), failureCode, now,
                state.terminal() ? Optional.of(now) : Optional.empty());
        store.update(current, replacement, metrics);
        publishStateChange(
                Optional.of(current.state()), replacement, event.requestContext());
    }

    private List<ProfileMetric> decodeAndValidate(
            ProfileJob job,
            ExecutionRequest execution,
            RequestContext requestContext
    ) {
        List<ProfileMetric> metrics = List.copyOf(decoders
                .require(execution.specification().workload().type())
                .decode(new ProfileResultDecodingRequest(job, execution, requestContext)));
        Set<String> expected = new HashSet<>(job.metrics().stream()
                .map(value -> value.key()).toList());
        Set<String> actual = new HashSet<>();
        for (ProfileMetric metric : metrics) {
            if (!metric.jobId().equals(job.jobId()) || !actual.add(metric.key())) {
                throw new IllegalStateException(
                        "profile result contains mismatched or duplicate metrics");
            }
        }
        if (!expected.equals(actual)) {
            throw new IllegalStateException("profile result does not match requested metrics");
        }
        return metrics;
    }

    private ProfileJob requireJob(ProfileJobId jobId) {
        return store.find(jobId)
                .orElseThrow(() -> new IllegalArgumentException("profile job does not exist"));
    }

    private static ProfileJobState mapState(ExecutionState state) {
        return switch (state) {
            case PENDING, DISPATCHING, QUEUED -> ProfileJobState.SUBMITTED;
            case RUNNING -> ProfileJobState.RUNNING;
            case SUCCEEDED -> ProfileJobState.SUCCEEDED;
            case FAILED -> ProfileJobState.FAILED;
            case TIMED_OUT -> ProfileJobState.TIMED_OUT;
            case CANCELLED -> ProfileJobState.CANCELLED;
        };
    }

    private static ProfileJob copy(
            ProfileJob current,
            ProfileJobState state,
            Optional<com.datausher.execution.api.ExecutionRequestId> executionRequestId,
            Optional<String> failureCode,
            Instant now,
            Optional<Instant> finishedAt
    ) {
        return new ProfileJob(
                current.jobId(), current.target(), current.metrics(), current.executionPolicy(),
                current.idempotencyKey(), state, executionRequestId, failureCode,
                current.attributes(), current.createdAt(), now, finishedAt,
                current.revision() + 1);
    }

    private void publishStateChange(
            Optional<ProfileJobState> previousState,
            ProfileJob job,
            RequestContext requestContext
    ) {
        eventPublisher.publish(new ProfileJobStateChangedEvent(
                nextEventId(), job.updatedAt(), requestContext, previousState, job));
    }

    private String nextEventId() {
        return idGenerator.nextIdValue(
                IdGenerationRequest.of("quality-profiler", "domain-event"));
    }
}
