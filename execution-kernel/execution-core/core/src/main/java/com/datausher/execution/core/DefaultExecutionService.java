package com.datausher.execution.core;

import com.datausher.execution.api.CancelExecutionRequest;
import com.datausher.execution.api.ExecutionAccount;
import com.datausher.execution.api.ExecutionAccountStatus;
import com.datausher.execution.api.ExecutionCommandService;
import com.datausher.execution.api.ExecutionEvents;
import com.datausher.execution.api.ExecutionExplainPlan;
import com.datausher.execution.api.ExecutionExplainService;
import com.datausher.execution.api.ExecutionFailure;
import com.datausher.execution.api.ExecutionInstance;
import com.datausher.execution.api.ExecutionInstanceId;
import com.datausher.execution.api.ExecutionLogEntry;
import com.datausher.execution.api.ExecutionLogPage;
import com.datausher.execution.api.ExecutionLogQueryService;
import com.datausher.execution.api.ExecutionQuery;
import com.datausher.execution.api.ExecutionQueryService;
import com.datausher.execution.api.ExecutionQueue;
import com.datausher.execution.api.ExecutionQueueId;
import com.datausher.execution.api.ExecutionQueueStatus;
import com.datausher.execution.api.ExecutionRequest;
import com.datausher.execution.api.ExecutionRequestId;
import com.datausher.execution.api.ExecutionResultColumn;
import com.datausher.execution.api.ExecutionResultMode;
import com.datausher.execution.api.ExecutionResultPage;
import com.datausher.execution.api.ExecutionResultQueryService;
import com.datausher.execution.api.ExecutionState;
import com.datausher.execution.api.ExecutionWorkloadType;
import com.datausher.execution.api.ExplainExecutionRequest;
import com.datausher.execution.api.ReadExecutionLogRequest;
import com.datausher.execution.api.ReadExecutionResultRequest;
import com.datausher.execution.api.SubmitExecutionRequest;
import com.datausher.integration.compute.api.ComputeCapabilities;
import com.datausher.integration.compute.api.ComputeEngineAdapter;
import com.datausher.integration.compute.api.ComputeJobHandle;
import com.datausher.integration.compute.api.ComputeJobLogPage;
import com.datausher.integration.compute.api.ComputeJobRequest;
import com.datausher.integration.compute.api.ComputeJobResultPage;
import com.datausher.integration.compute.api.ComputeJobState;
import com.datausher.integration.compute.api.ComputeJobStatus;
import com.datausher.integration.compute.api.SqlEngineAdapter;
import com.datausher.integration.compute.api.SqlExecutionRequest;
import com.datausher.integration.compute.api.SqlExplainPlan;
import com.datausher.integration.runtime.api.AdapterInvocationExecutor;
import com.datausher.integration.runtime.api.AdapterRegistry;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.IntegrationError;
import com.datausher.integration.runtime.api.IntegrationErrorMapper;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.BaseDomainEvent;
import com.datausher.platform.shared.event.DomainEventPublisher;
import com.datausher.platform.shared.id.IdGenerationRequest;
import com.datausher.platform.shared.id.IdGenerator;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;
import com.datausher.platform.shared.time.Clock;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class DefaultExecutionService
        implements ExecutionCommandService, ExecutionQueryService, ExecutionLogQueryService,
        ExecutionResultQueryService, ExecutionExplainService, ExecutionWorker {
    private static final String SOURCE_MODULE = "execution-core";

    private final ExecutionStore executionStore;
    private final ExecutionQueueStore queueStore;
    private final ExecutionAccountStore accountStore;
    private final AdapterRegistry adapterRegistry;
    private final AdapterInvocationExecutor invocationExecutor;
    private final IntegrationErrorMapper errorMapper;
    private final Clock clock;
    private final IdGenerator idGenerator;
    private final DomainEventPublisher eventPublisher;
    private final Duration adapterTimeout;

    public DefaultExecutionService(
            ExecutionStore executionStore,
            ExecutionQueueStore queueStore,
            ExecutionAccountStore accountStore,
            AdapterRegistry adapterRegistry,
            AdapterInvocationExecutor invocationExecutor,
            IntegrationErrorMapper errorMapper,
            Clock clock,
            IdGenerator idGenerator,
            DomainEventPublisher eventPublisher,
            Duration adapterTimeout
    ) {
        this.executionStore = Objects.requireNonNull(
                executionStore, "executionStore must not be null");
        this.queueStore = Objects.requireNonNull(queueStore, "queueStore must not be null");
        this.accountStore = Objects.requireNonNull(accountStore, "accountStore must not be null");
        this.adapterRegistry = Objects.requireNonNull(
                adapterRegistry, "adapterRegistry must not be null");
        this.invocationExecutor = Objects.requireNonNull(
                invocationExecutor, "invocationExecutor must not be null");
        this.errorMapper = Objects.requireNonNull(errorMapper, "errorMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.eventPublisher = Objects.requireNonNull(
                eventPublisher, "eventPublisher must not be null");
        this.adapterTimeout = Objects.requireNonNull(
                adapterTimeout, "adapterTimeout must not be null");
        if (adapterTimeout.isZero() || adapterTimeout.isNegative()) {
            throw new IllegalArgumentException("adapterTimeout must be positive");
        }
    }

    @Override
    public ExecutionRequest submit(SubmitExecutionRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        requireActiveQueue(request.queueId());
        ExecutionAccount account = requireActiveAccount(request.accountId());
        requireSupportedWorkload(account, request.workload().type());
        requireAdapter(account);
        Instant now = clock.now();
        ExecutionRequest execution = new ExecutionRequest(
                new ExecutionRequestId(nextId("execution-request")),
                request.queueId(),
                request.accountId(),
                request.workload(),
                request.resultMode(),
                request.resultPageSize(),
                ExecutionState.QUEUED,
                now,
                now,
                Optional.empty(),
                1
        );
        executionStore.create(new StoredExecution(execution, request.requestContext()));
        publish(ExecutionEvents.SUBMITTED, request.requestContext(), now);
        publish(ExecutionEvents.QUEUED, request.requestContext(), now);
        return execution;
    }

    @Override
    public ExecutionRequest cancel(CancelExecutionRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        StoredExecution execution = requireExecution(request.requestId());
        if (execution.request().revision() != request.expectedRevision()) {
            throw new IllegalStateException(
                    "execution revision does not match expectedRevision: " + request.requestId());
        }
        if (execution.request().state() == ExecutionState.CANCELLED) {
            return execution.request();
        }
        if (execution.request().state().terminal()) {
            throw new IllegalStateException(
                    "terminal execution cannot be cancelled: " + request.requestId());
        }
        Optional<StoredExecutionInstance> latest = latestInstance(request.requestId());
        Instant now = clock.now();
        StoredExecution cancelled = transition(
                execution, ExecutionState.CANCELLED, now, Optional.empty());
        if (latest.isEmpty()) {
            executionStore.update(execution, cancelled);
        } else {
            StoredExecutionInstance currentInstance = latest.get();
            currentInstance.handle().ifPresent(handle -> cancelExternal(
                    execution, account(execution.request()), handle, request.requestContext()));
            StoredExecutionInstance cancelledInstance = transition(
                    currentInstance, ExecutionState.CANCELLED, now, Optional.empty(),
                    currentInstance.handle());
            executionStore.update(execution, cancelled, currentInstance, cancelledInstance);
        }
        publish(ExecutionEvents.CANCELLED, request.requestContext(), now);
        return cancelled.request();
    }

    @Override
    public Optional<ExecutionInstance> dispatchNext(
            ExecutionQueueId queueId,
            RequestContext requestContext
    ) {
        Objects.requireNonNull(requestContext, "requestContext must not be null");
        ExecutionQueue queue = requireActiveQueue(queueId);
        Optional<ExecutionDispatch> claimed = executionStore.claimNext(
                queueId,
                queue.maxConcurrency(),
                new ExecutionInstanceId(nextId("execution-instance")),
                clock.now()
        );
        if (claimed.isEmpty()) {
            return Optional.empty();
        }
        ExecutionDispatch dispatch = claimed.get();
        ExecutionAccount account = account(dispatch.execution().request());
        ComputeEngineAdapter adapter = requireAdapter(account);
        try {
            AdapterRequestContext adapterContext = adapterContext(requestContext);
            ComputeJobHandle handle = invocationExecutor.execute(
                    adapterContext,
                    adapter,
                    "submit",
                    () -> adapter.submit(
                            adapterContext,
                            jobRequest(dispatch.execution().request(), account)
                    )
            );
            validateHandle(handle, account, adapter);
            StoredExecution latest = requireExecution(
                    dispatch.execution().request().requestId());
            StoredExecutionInstance latestInstance = requireInstance(
                    dispatch.instance().instance().instanceId());
            if (latest.request().state() == ExecutionState.CANCELLED) {
                cancelExternal(latest, account, handle, requestContext);
                return Optional.of(latestInstance.instance());
            }
            Instant startedAt = clock.now();
            StoredExecution running = transition(
                    latest, ExecutionState.RUNNING, startedAt, Optional.empty());
            StoredExecutionInstance runningInstance = transition(
                    latestInstance, ExecutionState.RUNNING, startedAt, Optional.empty(),
                    Optional.of(handle));
            executionStore.update(latest, running, latestInstance, runningInstance);
            publish(ExecutionEvents.STARTED, requestContext, startedAt);
            return Optional.of(runningInstance.instance());
        } catch (RuntimeException failure) {
            return Optional.of(failDispatch(dispatch, account, failure, requestContext));
        }
    }

    @Override
    public ExecutionInstance refresh(
            ExecutionInstanceId instanceId,
            RequestContext requestContext
    ) {
        Objects.requireNonNull(requestContext, "requestContext must not be null");
        StoredExecutionInstance storedInstance = requireInstance(instanceId);
        if (storedInstance.instance().state().terminal()) {
            return storedInstance.instance();
        }
        ComputeJobHandle handle = storedInstance.handle().orElseThrow(() ->
                new IllegalStateException("execution instance has no external handle: " + instanceId));
        StoredExecution execution = requireExecution(storedInstance.instance().requestId());
        ExecutionAccount account = account(execution.request());
        ComputeEngineAdapter adapter = requireAdapter(account);
        try {
            AdapterRequestContext adapterContext = adapterContext(requestContext);
            ComputeJobStatus status = invocationExecutor.execute(
                    adapterContext, adapter, "status",
                    () -> adapter.status(adapterContext, handle));
            validateStatus(status, handle);
            ExecutionState nextState = mapState(status.state(), storedInstance.instance().state());
            if (nextState == storedInstance.instance().state()) {
                return storedInstance.instance();
            }
            Instant changedAt = clock.now();
            Optional<ExecutionFailure> statusFailure = nextState == ExecutionState.FAILED
                    ? Optional.of(new ExecutionFailure(
                            "COMPUTE_JOB_FAILED",
                            status.message().isEmpty() ? "compute job failed" : status.message(),
                            false,
                            status.details()
                    ))
                    : Optional.empty();
            StoredExecution updatedExecution = transition(
                    execution, nextState, changedAt, statusFailure);
            StoredExecutionInstance updatedInstance = transition(
                    storedInstance, nextState, changedAt, statusFailure, Optional.of(handle));
            executionStore.update(
                    execution, updatedExecution, storedInstance, updatedInstance);
            publish(eventFor(nextState), requestContext, changedAt);
            return updatedInstance.instance();
        } catch (RuntimeException failure) {
            return fail(execution, storedInstance, account, failure, requestContext);
        }
    }

    @Override
    public Optional<ExecutionRequest> findRequest(ExecutionRequestId requestId) {
        return executionStore.find(Objects.requireNonNull(
                requestId, "requestId must not be null")).map(StoredExecution::request);
    }

    @Override
    public Optional<ExecutionInstance> findInstance(ExecutionInstanceId instanceId) {
        return executionStore.findInstance(Objects.requireNonNull(
                instanceId, "instanceId must not be null"))
                .map(StoredExecutionInstance::instance);
    }

    @Override
    public List<ExecutionInstance> listInstances(ExecutionRequestId requestId) {
        Objects.requireNonNull(requestId, "requestId must not be null");
        return executionStore.listInstances(requestId).stream()
                .map(StoredExecutionInstance::instance)
                .toList();
    }

    @Override
    public PageResult<ExecutionRequest> search(
            ExecutionQuery query,
            PageRequest pageRequest
    ) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        return executionStore.search(query, pageRequest);
    }

    @Override
    public ExecutionLogPage read(ReadExecutionLogRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        StoredExecutionInstance instance = requireInstance(request.instanceId());
        ComputeJobHandle handle = requireHandle(instance);
        StoredExecution execution = requireExecution(instance.instance().requestId());
        ExecutionAccount account = account(execution.request());
        ComputeEngineAdapter adapter = requireCapability(
                requireAdapter(account), ComputeCapabilities.JOB_LOGS);
        AdapterRequestContext adapterContext = adapterContext(request.requestContext());
        ComputeJobLogPage page = invocationExecutor.execute(
                adapterContext, adapter, "read-logs",
                () -> adapter.readLogs(
                        adapterContext, handle, request.afterSequence(), request.limit()));
        validateLogPage(page, handle, request.afterSequence(), request.limit());
        return new ExecutionLogPage(
                page.entries().stream()
                        .map(entry -> new ExecutionLogEntry(
                                entry.sequence(), entry.occurredAt(), entry.level(),
                                entry.message(), entry.attributes()))
                        .toList(),
                page.nextSequence(),
                page.complete()
        );
    }

    @Override
    public ExecutionResultPage read(ReadExecutionResultRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        StoredExecutionInstance instance = requireInstance(request.instanceId());
        ComputeJobHandle handle = requireHandle(instance);
        StoredExecution execution = requireExecution(instance.instance().requestId());
        if (execution.request().resultMode().equals(ExecutionResultMode.DISCARD)) {
            throw new IllegalStateException("execution result mode discards results");
        }
        ExecutionAccount account = account(execution.request());
        ComputeEngineAdapter adapter = requireCapability(
                requireAdapter(account), ComputeCapabilities.JOB_RESULTS);
        AdapterRequestContext adapterContext = adapterContext(request.requestContext());
        ComputeJobResultPage page = invocationExecutor.execute(
                adapterContext, adapter, "read-result",
                () -> adapter.readResult(
                        adapterContext, handle, request.offset(), request.limit()));
        validateResultPage(page, handle, request.offset(), request.limit());
        return new ExecutionResultPage(
                page.columns().stream()
                        .map(column -> new ExecutionResultColumn(
                                column.name(), column.type(), column.nullable(),
                                column.attributes()))
                        .toList(),
                page.rows().stream()
                        .map(row -> row.stream()
                                .map(ExecutionValueMapper::fromIntegration)
                                .toList())
                        .toList(),
                page.offset(),
                page.affectedRows(),
                page.hasMore(),
                page.resultReference(),
                page.attributes()
        );
    }

    @Override
    public ExecutionExplainPlan explain(ExplainExecutionRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (!request.workload().type().equals(ExecutionWorkloadType.SQL)) {
            throw new IllegalArgumentException("explain currently requires an SQL workload");
        }
        ExecutionAccount account = requireActiveAccount(request.accountId());
        requireSupportedWorkload(account, request.workload().type());
        SqlEngineAdapter adapter = adapterRegistry.find(
                account.adapterId(), SqlEngineAdapter.class).orElseThrow(() ->
                new IllegalStateException(
                        "SQL compute adapter is not registered: " + account.adapterId()));
        requireCapability(adapter, ComputeCapabilities.SQL_EXPLAIN);
        AdapterRequestContext adapterContext = adapterContext(request.requestContext());
        SqlExplainPlan plan = invocationExecutor.execute(
                adapterContext,
                adapter,
                "explain",
                () -> adapter.explain(adapterContext, new SqlExecutionRequest(
                        account.credentialBindingId(),
                        request.workload().payload(),
                        request.workload().parameters().entrySet().stream()
                                .collect(Collectors.toUnmodifiableMap(
                                        Map.Entry::getKey,
                                        entry -> ExecutionValueMapper.toIntegration(
                                                entry.getValue())
                                )),
                        1,
                        request.workload().options()
                ))
        );
        Objects.requireNonNull(plan, "SQL compute adapter returned a null explain plan");
        return new ExecutionExplainPlan(plan.format(), plan.content(), plan.attributes());
    }

    private ExecutionInstance failDispatch(
            ExecutionDispatch dispatch,
            ExecutionAccount account,
            RuntimeException failure,
            RequestContext requestContext
    ) {
        StoredExecution execution = requireExecution(
                dispatch.execution().request().requestId());
        StoredExecutionInstance instance = requireInstance(
                dispatch.instance().instance().instanceId());
        if (execution.request().state() == ExecutionState.CANCELLED) {
            return instance.instance();
        }
        return fail(execution, instance, account, failure, requestContext);
    }

    private ExecutionInstance fail(
            StoredExecution execution,
            StoredExecutionInstance instance,
            ExecutionAccount account,
            RuntimeException failure,
            RequestContext requestContext
    ) {
        IntegrationError error = errorMapper.map(account.adapterId(), failure);
        ExecutionState state = error.code()
                == com.datausher.integration.runtime.api.IntegrationErrorCode.TIMEOUT
                ? ExecutionState.TIMED_OUT
                : ExecutionState.FAILED;
        ExecutionFailure executionFailure = new ExecutionFailure(
                error.code().name(), error.message(), error.retryable(), error.details());
        Instant failedAt = clock.now();
        StoredExecution failedExecution = transition(
                execution, state, failedAt, Optional.of(executionFailure));
        StoredExecutionInstance failedInstance = transition(
                instance, state, failedAt, Optional.of(executionFailure), instance.handle());
        executionStore.update(execution, failedExecution, instance, failedInstance);
        publish(eventFor(state), requestContext, failedAt);
        return failedInstance.instance();
    }

    private void cancelExternal(
            StoredExecution execution,
            ExecutionAccount account,
            ComputeJobHandle handle,
            RequestContext requestContext
    ) {
        ComputeEngineAdapter adapter = requireCapability(
                requireAdapter(account), ComputeCapabilities.JOB_CANCELLATION);
        AdapterRequestContext adapterContext = adapterContext(requestContext);
        invocationExecutor.execute(
                adapterContext, adapter, "cancel",
                () -> adapter.cancel(adapterContext, handle));
    }

    private ExecutionQueue requireActiveQueue(ExecutionQueueId queueId) {
        ExecutionQueue queue = queueStore.find(Objects.requireNonNull(
                queueId, "queueId must not be null")).orElseThrow(() ->
                new IllegalArgumentException("execution queue does not exist: " + queueId));
        if (queue.status() != ExecutionQueueStatus.ACTIVE) {
            throw new IllegalStateException("execution queue is not active: " + queueId);
        }
        return queue;
    }

    private ExecutionAccount requireActiveAccount(
            com.datausher.execution.api.ExecutionAccountId accountId
    ) {
        ExecutionAccount account = accountStore.find(accountId).orElseThrow(() ->
                new IllegalArgumentException("execution account does not exist: " + accountId));
        if (account.status() != ExecutionAccountStatus.ACTIVE) {
            throw new IllegalStateException("execution account is not active: " + accountId);
        }
        return account;
    }

    private ExecutionAccount account(ExecutionRequest request) {
        return accountStore.find(request.accountId()).orElseThrow(() ->
                new IllegalStateException(
                        "execution account no longer exists: " + request.accountId()));
    }

    private ComputeEngineAdapter requireAdapter(ExecutionAccount account) {
        ComputeEngineAdapter adapter = adapterRegistry.find(
                account.adapterId(), ComputeEngineAdapter.class).orElseThrow(() ->
                new IllegalStateException(
                        "compute adapter is not registered: " + account.adapterId()));
        if (!adapter.descriptor().supports(ComputeCapabilities.JOB_EXECUTION)) {
            throw new IllegalStateException(
                    "compute adapter does not support job execution: " + account.adapterId());
        }
        return adapter;
    }

    private static <T extends ComputeEngineAdapter> T requireCapability(
            T adapter,
            String capability
    ) {
        if (!adapter.descriptor().supports(capability)) {
            throw new IllegalStateException(
                    "compute adapter does not support capability: " + capability);
        }
        return adapter;
    }

    private static void requireSupportedWorkload(
            ExecutionAccount account,
            com.datausher.execution.api.ExecutionWorkloadType workloadType
    ) {
        if (!account.supports(workloadType)) {
            throw new IllegalArgumentException(
                    "execution account does not support workload type: " + workloadType);
        }
    }

    private ComputeJobRequest jobRequest(
            ExecutionRequest request,
            ExecutionAccount account
    ) {
        return new ComputeJobRequest(
                account.credentialBindingId(),
                request.workload().type().value(),
                request.workload().payload(),
                request.workload().parameters().entrySet().stream()
                        .collect(Collectors.toUnmodifiableMap(
                                Map.Entry::getKey,
                                entry -> ExecutionValueMapper.toIntegration(entry.getValue())
                        )),
                request.workload().options()
        );
    }

    private AdapterRequestContext adapterContext(RequestContext requestContext) {
        return new AdapterRequestContext(
                requestContext.requestId(),
                clock.now().plus(adapterTimeout),
                requestContext.attributes()
        );
    }

    private StoredExecution requireExecution(ExecutionRequestId requestId) {
        return executionStore.find(requestId).orElseThrow(() ->
                new IllegalArgumentException("execution request does not exist: " + requestId));
    }

    private StoredExecutionInstance requireInstance(ExecutionInstanceId instanceId) {
        return executionStore.findInstance(instanceId).orElseThrow(() ->
                new IllegalArgumentException("execution instance does not exist: " + instanceId));
    }

    private Optional<StoredExecutionInstance> latestInstance(ExecutionRequestId requestId) {
        List<StoredExecutionInstance> values = executionStore.listInstances(requestId);
        return values.isEmpty() ? Optional.empty() : Optional.of(values.getLast());
    }

    private static StoredExecution transition(
            StoredExecution execution,
            ExecutionState state,
            Instant changedAt,
            Optional<ExecutionFailure> failure
    ) {
        return new StoredExecution(InMemoryExecutionStore.transitionRequest(
                execution.request(), state, changedAt, failure), execution.requestContext());
    }

    private static StoredExecutionInstance transition(
            StoredExecutionInstance stored,
            ExecutionState state,
            Instant changedAt,
            Optional<ExecutionFailure> failure,
            Optional<ComputeJobHandle> handle
    ) {
        ExecutionInstance current = stored.instance();
        Optional<Instant> startedAt = current.startedAt();
        if (state == ExecutionState.RUNNING && startedAt.isEmpty()) {
            startedAt = Optional.of(changedAt);
        }
        Optional<Instant> finishedAt = state.terminal()
                ? Optional.of(changedAt)
                : Optional.empty();
        return new StoredExecutionInstance(new ExecutionInstance(
                current.instanceId(), current.requestId(), current.attempt(), state,
                current.createdAt(), startedAt, finishedAt, failure, current.revision() + 1),
                handle);
    }

    private static ExecutionState mapState(
            ComputeJobState state,
            ExecutionState current
    ) {
        return switch (state) {
            case QUEUED -> current == ExecutionState.RUNNING
                    ? current
                    : ExecutionState.QUEUED;
            case RUNNING -> ExecutionState.RUNNING;
            case SUCCEEDED -> ExecutionState.SUCCEEDED;
            case FAILED -> ExecutionState.FAILED;
            case CANCELLED -> ExecutionState.CANCELLED;
            case UNKNOWN -> current;
        };
    }

    private static String eventFor(ExecutionState state) {
        return switch (state) {
            case SUCCEEDED -> ExecutionEvents.COMPLETED;
            case FAILED -> ExecutionEvents.FAILED;
            case CANCELLED -> ExecutionEvents.CANCELLED;
            case TIMED_OUT -> ExecutionEvents.TIMED_OUT;
            case RUNNING -> ExecutionEvents.STARTED;
            case QUEUED -> ExecutionEvents.QUEUED;
            case PENDING, DISPATCHING -> throw new IllegalArgumentException(
                    "state has no public lifecycle event: " + state);
        };
    }

    private static void validateHandle(
            ComputeJobHandle handle,
            ExecutionAccount account,
            ComputeEngineAdapter adapter
    ) {
        Objects.requireNonNull(handle, "compute adapter returned a null handle");
        if (!handle.adapterId().equals(adapter.descriptor().adapterId())
                || !handle.bindingId().equals(account.credentialBindingId())) {
            throw new IllegalStateException(
                    "compute adapter returned a handle with mismatched ownership");
        }
    }

    private static void validateStatus(
            ComputeJobStatus status,
            ComputeJobHandle handle
    ) {
        Objects.requireNonNull(status, "compute adapter returned a null status");
        if (!status.handle().equals(handle)) {
            throw new IllegalStateException(
                    "compute adapter returned status for a different handle");
        }
    }

    private static ComputeJobHandle requireHandle(StoredExecutionInstance instance) {
        return instance.handle().orElseThrow(() -> new IllegalStateException(
                "execution instance has no external handle: "
                        + instance.instance().instanceId()));
    }

    private static void validateLogPage(
            ComputeJobLogPage page,
            ComputeJobHandle handle,
            long afterSequence,
            int limit
    ) {
        Objects.requireNonNull(page, "compute adapter returned a null log page");
        if (!page.handle().equals(handle)) {
            throw new IllegalStateException("compute adapter returned logs for a different handle");
        }
        if (page.entries().size() > limit
                || page.entries().stream().anyMatch(entry -> entry.sequence() <= afterSequence)
                || page.nextSequence() <= afterSequence) {
            throw new IllegalStateException("compute adapter returned an invalid log page");
        }
    }

    private static void validateResultPage(
            ComputeJobResultPage page,
            ComputeJobHandle handle,
            long offset,
            int limit
    ) {
        Objects.requireNonNull(page, "compute adapter returned a null result page");
        if (!page.handle().equals(handle)) {
            throw new IllegalStateException(
                    "compute adapter returned results for a different handle");
        }
        if (page.offset() != offset || page.rows().size() > limit) {
            throw new IllegalStateException("compute adapter returned an invalid result page");
        }
    }

    private void publish(String eventType, RequestContext context, Instant occurredAt) {
        eventPublisher.publish(new BaseDomainEvent(
                nextId("domain-event"), eventType, SOURCE_MODULE, occurredAt, context));
    }

    private String nextId(String entityType) {
        return idGenerator.nextIdValue(IdGenerationRequest.of(SOURCE_MODULE, entityType));
    }
}
