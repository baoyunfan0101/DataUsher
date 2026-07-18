package com.datausher.workflow.core;

import com.datausher.integration.runtime.api.AdapterInvocationExecutor;
import com.datausher.integration.runtime.api.AdapterRegistry;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.scheduler.api.PublishedWorkflow;
import com.datausher.integration.scheduler.api.WorkflowRunHandle;
import com.datausher.integration.scheduler.api.WorkflowRunState;
import com.datausher.integration.scheduler.api.WorkflowRunStatus;
import com.datausher.integration.scheduler.api.WorkflowSchedulerAdapter;
import com.datausher.integration.scheduler.api.WorkflowTaskRunPage;
import com.datausher.integration.scheduler.api.WorkflowTaskRunStatus;
import com.datausher.integration.scheduler.api.WorkflowTrigger;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.time.Clock;
import com.datausher.workflow.api.TaskInstanceState;
import com.datausher.workflow.api.WorkflowInstance;
import com.datausher.workflow.api.WorkflowInstanceState;
import com.datausher.workflow.api.WorkflowPublication;
import com.datausher.workflow.api.WorkflowPublicationService;
import com.datausher.workflow.api.WorkflowRunReference;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class DefaultSchedulerManagedWorkflowGateway
        implements SchedulerManagedWorkflowGateway {
    private final WorkflowPublicationService publications;
    private final AdapterRegistry adapters;
    private final AdapterInvocationExecutor invocationExecutor;
    private final Clock clock;
    private final Duration adapterTimeout;
    private final int taskPageSize;
    private final int maxTaskPages;

    public DefaultSchedulerManagedWorkflowGateway(
            WorkflowPublicationService publications,
            AdapterRegistry adapters,
            AdapterInvocationExecutor invocationExecutor,
            Clock clock,
            Duration adapterTimeout,
            int taskPageSize,
            int maxTaskPages
    ) {
        this.publications = Objects.requireNonNull(publications, "publications must not be null");
        this.adapters = Objects.requireNonNull(adapters, "adapters must not be null");
        this.invocationExecutor = Objects.requireNonNull(
                invocationExecutor, "invocationExecutor must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.adapterTimeout = Objects.requireNonNull(
                adapterTimeout, "adapterTimeout must not be null");
        if (adapterTimeout.isZero() || adapterTimeout.isNegative()
                || taskPageSize < 1 || maxTaskPages < 1) {
            throw new IllegalArgumentException(
                    "positive timeout, taskPageSize, and maxTaskPages are required");
        }
        this.taskPageSize = taskPageSize;
        this.maxTaskPages = maxTaskPages;
    }

    @Override
    public WorkflowRunReference trigger(
            WorkflowInstance instance,
            RequestContext requestContext
    ) {
        WorkflowPublication publication = requirePublication(instance);
        WorkflowSchedulerAdapter adapter = requireAdapter(instance);
        AdapterRequestContext adapterContext = adapterContext(requestContext);
        WorkflowTrigger trigger = new WorkflowTrigger(
                toPublishedWorkflow(publication), instance.idempotencyKey(),
                instance.parameters().entrySet().stream().collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> WorkflowIntegrationValues.convert(entry.getValue()))));
        WorkflowRunHandle handle = invocationExecutor.execute(
                adapterContext, adapter, "trigger",
                () -> adapter.trigger(adapterContext, trigger));
        validateHandle(handle, instance, adapter);
        return new WorkflowRunReference(
                handle.adapterId(), handle.bindingId(), handle.externalRunId(), Map.of());
    }

    @Override
    public SchedulerManagedWorkflowObservation observe(
            WorkflowInstance instance,
            RequestContext requestContext
    ) {
        WorkflowSchedulerAdapter adapter = requireAdapter(instance);
        WorkflowRunHandle handle = toHandle(instance);
        AdapterRequestContext adapterContext = adapterContext(requestContext);
        WorkflowRunStatus status = invocationExecutor.execute(
                adapterContext, adapter, "status",
                () -> adapter.status(adapterContext, handle));
        validateStatus(status, handle);
        List<WorkflowTaskRunStatus> taskRuns = readAllTaskRuns(adapter, adapterContext, handle);
        Map<String, WorkflowTaskRunStatus> latestByTask = new HashMap<>();
        for (WorkflowTaskRunStatus taskRun : taskRuns) {
            latestByTask.merge(taskRun.taskKey(), taskRun,
                    (current, candidate) -> Comparator
                            .comparingInt(WorkflowTaskRunStatus::attempt)
                            .compare(current, candidate) < 0 ? candidate : current);
        }
        List<SchedulerManagedTaskObservation> tasks = latestByTask.values().stream()
                .map(DefaultSchedulerManagedWorkflowGateway::toTaskObservation)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(SchedulerManagedTaskObservation::taskKey))
                .toList();
        return new SchedulerManagedWorkflowObservation(
                mapWorkflowState(status.state()), status.observedAt(), tasks);
    }

    @Override
    public void cancel(WorkflowInstance instance, RequestContext requestContext) {
        WorkflowSchedulerAdapter adapter = requireAdapter(instance);
        WorkflowRunHandle handle = toHandle(instance);
        AdapterRequestContext adapterContext = adapterContext(requestContext);
        invocationExecutor.execute(adapterContext, adapter, "cancel", () -> {
            adapter.cancel(adapterContext, handle);
            return null;
        });
    }

    private List<WorkflowTaskRunStatus> readAllTaskRuns(
            WorkflowSchedulerAdapter adapter,
            AdapterRequestContext adapterContext,
            WorkflowRunHandle handle
    ) {
        List<WorkflowTaskRunStatus> result = new ArrayList<>();
        String cursor = "";
        for (int pageNumber = 0; pageNumber < maxTaskPages; pageNumber++) {
            String requestedCursor = cursor;
            WorkflowTaskRunPage page = invocationExecutor.execute(
                    adapterContext, adapter, "read-task-runs",
                    () -> adapter.readTaskRuns(
                            adapterContext, handle, requestedCursor, taskPageSize));
            Objects.requireNonNull(page, "scheduler adapter returned null task page");
            if (!page.handle().equals(handle)) {
                throw new IllegalStateException("scheduler adapter returned mismatched task page handle");
            }
            result.addAll(page.items());
            if (page.complete()) {
                return List.copyOf(result);
            }
            if (page.nextCursor().isEmpty() || page.nextCursor().equals(cursor)) {
                throw new IllegalStateException("scheduler adapter returned invalid task page cursor");
            }
            cursor = page.nextCursor();
        }
        throw new IllegalStateException("scheduler task observation exceeded configured page limit");
    }

    private WorkflowPublication requirePublication(WorkflowInstance instance) {
        return publications.findPublication(instance.workflowId(), instance.workflowVersion())
                .orElseThrow(() -> new IllegalStateException(
                        "scheduler-managed workflow version has not been published"));
    }

    private WorkflowSchedulerAdapter requireAdapter(WorkflowInstance instance) {
        String adapterId = instance.runtimeBinding().adapterId().orElseThrow();
        return adapters.find(adapterId, WorkflowSchedulerAdapter.class)
                .orElseThrow(() -> new IllegalStateException(
                        "workflow scheduler adapter does not exist: " + adapterId));
    }

    private AdapterRequestContext adapterContext(RequestContext requestContext) {
        return new AdapterRequestContext(
                requestContext.requestId(), clock.now().plus(adapterTimeout), Map.of());
    }

    private static PublishedWorkflow toPublishedWorkflow(WorkflowPublication publication) {
        return new PublishedWorkflow(
                publication.adapterId(), publication.bindingId(), publication.workflowId().value(),
                publication.idempotencyKey(), publication.externalWorkflowId(),
                publication.externalRevision());
    }

    private static WorkflowRunHandle toHandle(WorkflowInstance instance) {
        WorkflowRunReference reference = instance.runReference()
                .orElseThrow(() -> new IllegalStateException(
                        "scheduler-managed workflow has not been triggered"));
        return new WorkflowRunHandle(
                reference.adapterId(), reference.bindingId(), instance.workflowId().value(),
                instance.idempotencyKey(), reference.externalRunId());
    }

    private static void validateHandle(
            WorkflowRunHandle handle,
            WorkflowInstance instance,
            WorkflowSchedulerAdapter adapter
    ) {
        Objects.requireNonNull(handle, "scheduler adapter returned null run handle");
        if (!handle.adapterId().equals(adapter.descriptor().adapterId())
                || !handle.bindingId().equals(
                instance.runtimeBinding().bindingId().orElseThrow())
                || !handle.workflowId().equals(instance.workflowId().value())
                || !handle.idempotencyKey().equals(instance.idempotencyKey())) {
            throw new IllegalStateException("scheduler adapter returned mismatched run identity");
        }
    }

    private static void validateStatus(WorkflowRunStatus status, WorkflowRunHandle handle) {
        Objects.requireNonNull(status, "scheduler adapter returned null run status");
        if (!status.handle().equals(handle)) {
            throw new IllegalStateException("scheduler adapter returned mismatched run status handle");
        }
    }

    private static Optional<WorkflowInstanceState> mapWorkflowState(WorkflowRunState state) {
        return switch (state) {
            case QUEUED, RUNNING -> Optional.of(WorkflowInstanceState.RUNNING);
            case SUCCEEDED -> Optional.of(WorkflowInstanceState.SUCCEEDED);
            case FAILED -> Optional.of(WorkflowInstanceState.FAILED);
            case TIMED_OUT -> Optional.of(WorkflowInstanceState.TIMED_OUT);
            case CANCELLED -> Optional.of(WorkflowInstanceState.CANCELLED);
            case UNKNOWN -> Optional.empty();
        };
    }

    private static Optional<SchedulerManagedTaskObservation> toTaskObservation(
            WorkflowTaskRunStatus status
    ) {
        TaskInstanceState state = switch (status.state()) {
            case QUEUED -> TaskInstanceState.QUEUED;
            case RUNNING -> TaskInstanceState.RUNNING;
            case SUCCEEDED -> TaskInstanceState.SUCCEEDED;
            case FAILED -> TaskInstanceState.FAILED;
            case TIMED_OUT -> TaskInstanceState.TIMED_OUT;
            case CANCELLED -> TaskInstanceState.CANCELLED;
            case UNKNOWN -> null;
        };
        if (state == null) {
            return Optional.empty();
        }
        Optional<String> failureCode = Optional.ofNullable(status.details().get("failureCode"));
        if (failureCode.isEmpty() && state == TaskInstanceState.FAILED) {
            failureCode = Optional.of("scheduler-task-failed");
        } else if (failureCode.isEmpty() && state == TaskInstanceState.TIMED_OUT) {
            failureCode = Optional.of("scheduler-task-timeout");
        }
        return Optional.of(new SchedulerManagedTaskObservation(
                status.taskKey(), status.attempt(), state, failureCode, status.finishedAt()));
    }
}
