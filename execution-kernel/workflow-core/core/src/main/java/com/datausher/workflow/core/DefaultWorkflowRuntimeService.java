package com.datausher.workflow.core;

import com.datausher.execution.api.CancelExecutionRequest;
import com.datausher.execution.api.ExecutionCommandService;
import com.datausher.execution.api.ExecutionOrigin;
import com.datausher.execution.api.ExecutionOriginType;
import com.datausher.execution.api.ExecutionQueryService;
import com.datausher.execution.api.ExecutionRequest;
import com.datausher.execution.api.ExecutionSpecification;
import com.datausher.execution.api.ExecutionState;
import com.datausher.execution.api.ExecutionStateChangedEvent;
import com.datausher.execution.api.ExecutionWorkload;
import com.datausher.execution.api.SubmitExecutionRequest;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEventPublisher;
import com.datausher.platform.shared.id.IdGenerationRequest;
import com.datausher.platform.shared.id.IdGenerator;
import com.datausher.platform.shared.time.Clock;
import com.datausher.workflow.api.CancelWorkflowRequest;
import com.datausher.workflow.api.TaskDependency;
import com.datausher.workflow.api.TaskDependencyCondition;
import com.datausher.workflow.api.TaskInstance;
import com.datausher.workflow.api.TaskInstanceId;
import com.datausher.workflow.api.TaskInstanceQueryService;
import com.datausher.workflow.api.TaskInstanceState;
import com.datausher.workflow.api.TriggerWorkflowRequest;
import com.datausher.workflow.api.WorkflowInstance;
import com.datausher.workflow.api.WorkflowInstanceId;
import com.datausher.workflow.api.WorkflowInstanceState;
import com.datausher.workflow.api.WorkflowInstanceStateChangedEvent;
import com.datausher.workflow.api.WorkflowId;
import com.datausher.workflow.api.WorkflowQueryService;
import com.datausher.workflow.api.WorkflowRuntimeService;
import com.datausher.workflow.api.WorkflowTaskDefinition;
import com.datausher.workflow.api.WorkflowTriggeredEvent;
import com.datausher.workflow.api.WorkflowVersion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class DefaultWorkflowRuntimeService
        implements WorkflowRuntimeService, TaskInstanceQueryService, WorkflowWorker {
    private final WorkflowQueryService workflows;
    private final WorkflowRuntimeStore store;
    private final ExecutionCommandService executions;
    private final ExecutionQueryService executionQueries;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final DomainEventPublisher eventPublisher;

    public DefaultWorkflowRuntimeService(
            WorkflowQueryService workflows,
            WorkflowRuntimeStore store,
            ExecutionCommandService executions,
            ExecutionQueryService executionQueries,
            IdGenerator idGenerator,
            Clock clock,
            DomainEventPublisher eventPublisher
    ) {
        this.workflows = Objects.requireNonNull(workflows, "workflows must not be null");
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.executions = Objects.requireNonNull(executions, "executions must not be null");
        this.executionQueries = Objects.requireNonNull(executionQueries, "executionQueries must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    @Override
    public WorkflowInstance trigger(TriggerWorkflowRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        WorkflowVersion version = requireVersion(request.workflowId(), request.version());
        Instant now = clock.now();
        WorkflowInstanceId instanceId = new WorkflowInstanceId(nextId("workflow-instance"));
        Set<String> downstreamKeys = version.specification().dependencies().stream()
                .map(TaskDependency::downstreamTaskKey).collect(Collectors.toSet());
        List<TaskInstance> tasks = version.specification().tasks().stream()
                .map(definition -> new TaskInstance(
                        new TaskInstanceId(nextId("task-instance")), instanceId, definition.taskKey(), 1,
                        downstreamKeys.contains(definition.taskKey())
                                ? TaskInstanceState.WAITING : TaskInstanceState.READY,
                        Optional.empty(), Optional.empty(), Optional.empty(), now, now, Optional.empty(), 1))
                .toList();
        WorkflowInstance instance = new WorkflowInstance(
                instanceId, request.workflowId(), request.version(), request.idempotencyKey(),
                request.parameters(), WorkflowInstanceState.PENDING, now, now, Optional.empty(), 1);
        WorkflowRunCreateResult creation = store.createOrFind(
                new StoredWorkflowRun(instance, tasks, request.requestContext()));
        WorkflowInstance existing = creation.run().instance();
        if (!creation.created()
                && (!existing.workflowId().equals(request.workflowId())
                || existing.workflowVersion() != request.version()
                || !existing.parameters().equals(request.parameters()))) {
            throw new IllegalStateException("workflow idempotency key was used for a different trigger");
        }
        if (creation.created()) {
            eventPublisher.publish(new WorkflowTriggeredEvent(
                    nextId("domain-event"), now, request.requestContext(), existing));
        }
        return existing;
    }

    @Override
    public WorkflowInstance dispatchReady(WorkflowInstanceId instanceId, RequestContext requestContext) {
        Objects.requireNonNull(requestContext, "requestContext must not be null");
        StoredWorkflowRun current = requireRun(instanceId);
        if (current.instance().state().terminal()) {
            return current.instance();
        }
        WorkflowVersion version = requireVersion(
                current.instance().workflowId(), current.instance().workflowVersion());
        Map<String, WorkflowTaskDefinition> definitions = version.specification().tasks().stream()
                .collect(Collectors.toMap(WorkflowTaskDefinition::taskKey, definition -> definition));
        Instant now = clock.now();
        List<TaskInstance> promoted = current.tasks().stream()
                .map(task -> task.state() == TaskInstanceState.RETRY_WAIT
                        && !task.nextEligibleAt().orElseThrow().isAfter(now)
                        ? copyTask(task, TaskInstanceState.READY, task.attempt(), Optional.empty(),
                        Optional.empty(), task.failureCode(), now, Optional.empty()) : task)
                .toList();
        StoredWorkflowRun latest = promoted.equals(current.tasks()) ? current
                : replaceRun(current, runningInstance(current.instance(), now), promoted, requestContext);
        for (TaskInstance task : List.copyOf(latest.tasks())) {
            if (task.state() != TaskInstanceState.READY) {
                continue;
            }
            WorkflowTaskDefinition definition = definitions.get(task.taskKey());
            ExecutionSpecification specification = withParameters(
                    definition.executionSpecification(), latest.instance().parameters());
            ExecutionRequest execution = executions.submit(new SubmitExecutionRequest(
                    specification,
                    executionKey(latest.instance(), task),
                    new ExecutionOrigin(
                            ExecutionOriginType.WORKFLOW_TASK, task.taskInstanceId().value(),
                            Integer.toString(task.attempt()),
                            Map.of("workflowInstanceId", latest.instance().instanceId().value(),
                                    "taskKey", task.taskKey())),
                    requestContext));
            TaskInstance queued = copyTask(
                    task, TaskInstanceState.QUEUED, task.attempt(), Optional.of(execution.requestId()),
                    Optional.empty(), Optional.empty(), clock.now(), Optional.empty());
            List<TaskInstance> updatedTasks = replaceTask(latest.tasks(), queued);
            latest = replaceRun(
                    latest, runningInstance(latest.instance(), clock.now()), updatedTasks, requestContext);
        }
        return latest.instance();
    }

    @Override
    public void handleExecutionStateChanged(ExecutionStateChangedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        ExecutionRequest execution = event.executionRequest();
        if (!execution.origin().type().equals(ExecutionOriginType.WORKFLOW_TASK)) {
            return;
        }
        TaskInstanceId taskInstanceId = new TaskInstanceId(execution.origin().originId());
        StoredWorkflowRun current = store.findByTaskInstanceId(taskInstanceId).orElse(null);
        if (current == null || current.instance().state().terminal()) {
            return;
        }
        TaskInstance task = findTask(current.tasks(), taskInstanceId);
        if (!execution.origin().attemptKey().equals(Integer.toString(task.attempt()))) {
            return;
        }
        TaskInstanceState state = mapState(execution.state());
        if (state == task.state() || state == null) {
            return;
        }
        WorkflowVersion version = requireVersion(
                current.instance().workflowId(), current.instance().workflowVersion());
        WorkflowTaskDefinition definition = version.specification().tasks().stream()
                .filter(candidate -> candidate.taskKey().equals(task.taskKey())).findFirst().orElseThrow();
        Instant now = event.occurredAt();
        Optional<String> failureCode = execution.failure().map(value -> value.code());
        TaskInstance updatedTask;
        if ((state == TaskInstanceState.FAILED || state == TaskInstanceState.CANCELLED)
                && retryable(definition, task, failureCode)) {
            Instant eligibleAt = now.plus(definition.retryPolicy().delay());
            TaskInstanceState retryState = definition.retryPolicy().delay().isZero()
                    ? TaskInstanceState.READY : TaskInstanceState.RETRY_WAIT;
            updatedTask = copyTask(
                    task, retryState, task.attempt() + 1, Optional.empty(),
                    retryState == TaskInstanceState.RETRY_WAIT
                            ? Optional.of(eligibleAt) : Optional.empty(),
                    failureCode, now, Optional.empty());
        } else {
            updatedTask = copyTask(
                    task, state, task.attempt(), Optional.of(execution.requestId()), Optional.empty(),
                    failureCode, now, state.terminal() ? Optional.of(now) : Optional.empty());
        }
        List<TaskInstance> tasks = replaceTask(current.tasks(), updatedTask);
        tasks = advanceDependencies(tasks, version, now);
        WorkflowInstance instance = aggregate(current.instance(), tasks, now);
        replaceRun(current, instance, tasks, event.requestContext());
    }

    @Override
    public WorkflowInstance cancel(CancelWorkflowRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        StoredWorkflowRun current = requireRun(request.instanceId());
        if (current.instance().revision() != request.expectedRevision()) {
            throw new IllegalStateException("workflow revision does not match expectedRevision");
        }
        if (current.instance().state() == WorkflowInstanceState.CANCELLED) {
            return current.instance();
        }
        if (current.instance().state().terminal()) {
            throw new IllegalStateException("terminal workflow cannot be cancelled");
        }
        Instant now = clock.now();
        List<TaskInstance> tasks = current.tasks().stream()
                .map(task -> task.state().terminal() ? task : copyTask(
                        task, TaskInstanceState.CANCELLED, task.attempt(), task.executionRequestId(),
                        Optional.empty(), task.failureCode(), now, Optional.of(now)))
                .toList();
        WorkflowInstance cancelled = copyInstance(
                current.instance(), WorkflowInstanceState.CANCELLED, now, Optional.of(now));
        store.update(current, new StoredWorkflowRun(cancelled, tasks, current.requestContext()));
        publishStateChange(current.instance(), cancelled, request.requestContext(), now);
        for (TaskInstance task : current.tasks()) {
            task.executionRequestId().flatMap(executionQueries::findRequest)
                    .filter(execution -> !execution.state().terminal())
                    .ifPresent(execution -> executions.cancel(new CancelExecutionRequest(
                            execution.requestId(), execution.revision(), request.requestContext())));
        }
        return cancelled;
    }

    @Override
    public WorkflowInstance refresh(WorkflowInstanceId instanceId, RequestContext requestContext) {
        Objects.requireNonNull(requestContext, "requestContext must not be null");
        return requireRun(Objects.requireNonNull(instanceId, "instanceId must not be null")).instance();
    }

    @Override
    public Optional<WorkflowInstance> findInstance(WorkflowInstanceId instanceId) {
        return store.find(Objects.requireNonNull(instanceId, "instanceId must not be null"))
                .map(StoredWorkflowRun::instance);
    }

    @Override
    public Optional<TaskInstance> findTaskInstance(TaskInstanceId taskInstanceId) {
        return store.findByTaskInstanceId(
                        Objects.requireNonNull(taskInstanceId, "taskInstanceId must not be null"))
                .map(run -> findTask(run.tasks(), taskInstanceId));
    }

    @Override
    public List<TaskInstance> listTaskInstances(WorkflowInstanceId workflowInstanceId) {
        return store.find(Objects.requireNonNull(
                        workflowInstanceId, "workflowInstanceId must not be null"))
                .map(StoredWorkflowRun::tasks).orElse(List.of());
    }

    private StoredWorkflowRun replaceRun(
            StoredWorkflowRun current,
            WorkflowInstance instance,
            List<TaskInstance> tasks,
            RequestContext requestContext
    ) {
        StoredWorkflowRun replacement = new StoredWorkflowRun(instance, tasks, current.requestContext());
        store.update(current, replacement);
        publishStateChange(current.instance(), instance, requestContext, instance.updatedAt());
        return replacement;
    }

    private void publishStateChange(
            WorkflowInstance previous,
            WorkflowInstance current,
            RequestContext requestContext,
            Instant occurredAt
    ) {
        if (previous.state() != current.state()) {
            eventPublisher.publish(new WorkflowInstanceStateChangedEvent(
                    nextId("domain-event"), occurredAt, requestContext, previous.state(), current));
        }
    }

    private List<TaskInstance> advanceDependencies(
            List<TaskInstance> tasks,
            WorkflowVersion version,
            Instant now
    ) {
        List<TaskInstance> result = new ArrayList<>(tasks);
        boolean changed;
        do {
            changed = false;
            Map<String, TaskInstance> byKey = result.stream()
                    .collect(Collectors.toMap(TaskInstance::taskKey, task -> task));
            for (TaskInstance task : List.copyOf(result)) {
                if (task.state() != TaskInstanceState.WAITING) {
                    continue;
                }
                List<TaskDependency> dependencies = version.specification().dependencies().stream()
                        .filter(value -> value.downstreamTaskKey().equals(task.taskKey())).toList();
                if (!dependencies.stream().allMatch(
                        value -> byKey.get(value.upstreamTaskKey()).state().terminal())) {
                    continue;
                }
                boolean satisfied = dependencies.stream().allMatch(value -> conditionSatisfied(
                        value.condition(), byKey.get(value.upstreamTaskKey()).state()));
                TaskInstance advanced = copyTask(
                        task, satisfied ? TaskInstanceState.READY : TaskInstanceState.SKIPPED,
                        task.attempt(), Optional.empty(), Optional.empty(), Optional.empty(), now,
                        satisfied ? Optional.empty() : Optional.of(now));
                result = new ArrayList<>(replaceTask(result, advanced));
                changed = true;
            }
        } while (changed);
        return List.copyOf(result);
    }

    private static boolean conditionSatisfied(TaskDependencyCondition condition, TaskInstanceState state) {
        if (condition.equals(TaskDependencyCondition.ALWAYS)) {
            return true;
        }
        if (condition.equals(TaskDependencyCondition.ON_SUCCESS)) {
            return state == TaskInstanceState.SUCCEEDED;
        }
        if (condition.equals(TaskDependencyCondition.ON_FAILURE)) {
            return state == TaskInstanceState.FAILED || state == TaskInstanceState.CANCELLED;
        }
        return false;
    }

    private static boolean retryable(
            WorkflowTaskDefinition definition,
            TaskInstance task,
            Optional<String> failureCode
    ) {
        if (task.attempt() >= definition.retryPolicy().maxAttempts()) {
            return false;
        }
        Set<String> codes = definition.retryPolicy().retryableFailureCodes();
        return codes.isEmpty() || failureCode.filter(codes::contains).isPresent();
    }

    private static WorkflowInstance aggregate(
            WorkflowInstance instance,
            List<TaskInstance> tasks,
            Instant now
    ) {
        if (!tasks.stream().allMatch(task -> task.state().terminal())) {
            return runningInstance(instance, now);
        }
        WorkflowInstanceState state = tasks.stream().anyMatch(task ->
                task.state() == TaskInstanceState.FAILED || task.state() == TaskInstanceState.CANCELLED)
                ? WorkflowInstanceState.FAILED : WorkflowInstanceState.SUCCEEDED;
        return copyInstance(instance, state, now, Optional.of(now));
    }

    private static WorkflowInstance runningInstance(WorkflowInstance instance, Instant now) {
        return instance.state() == WorkflowInstanceState.RUNNING
                ? copyInstance(instance, WorkflowInstanceState.RUNNING, now, Optional.empty())
                : copyInstance(instance, WorkflowInstanceState.RUNNING, now, Optional.empty());
    }

    private static WorkflowInstance copyInstance(
            WorkflowInstance instance,
            WorkflowInstanceState state,
            Instant now,
            Optional<Instant> finishedAt
    ) {
        return new WorkflowInstance(
                instance.instanceId(), instance.workflowId(), instance.workflowVersion(),
                instance.idempotencyKey(), instance.parameters(), state,
                instance.createdAt(), now, finishedAt, instance.revision() + 1);
    }

    private static TaskInstance copyTask(
            TaskInstance task,
            TaskInstanceState state,
            int attempt,
            Optional<com.datausher.execution.api.ExecutionRequestId> executionRequestId,
            Optional<Instant> nextEligibleAt,
            Optional<String> failureCode,
            Instant now,
            Optional<Instant> finishedAt
    ) {
        return new TaskInstance(
                task.taskInstanceId(), task.workflowInstanceId(), task.taskKey(), attempt, state,
                executionRequestId, nextEligibleAt, failureCode, task.createdAt(), now,
                finishedAt, task.revision() + 1);
    }

    private static List<TaskInstance> replaceTask(List<TaskInstance> tasks, TaskInstance replacement) {
        List<TaskInstance> updated = new ArrayList<>(tasks);
        for (int index = 0; index < updated.size(); index++) {
            if (updated.get(index).taskInstanceId().equals(replacement.taskInstanceId())) {
                updated.set(index, replacement);
                return List.copyOf(updated);
            }
        }
        throw new IllegalArgumentException("task instance does not belong to workflow run");
    }

    private static TaskInstance findTask(List<TaskInstance> tasks, TaskInstanceId taskInstanceId) {
        return tasks.stream().filter(task -> task.taskInstanceId().equals(taskInstanceId))
                .findFirst().orElseThrow();
    }

    private static ExecutionSpecification withParameters(
            ExecutionSpecification specification,
            Map<String, com.datausher.execution.api.ExecutionValue> workflowParameters
    ) {
        Map<String, com.datausher.execution.api.ExecutionValue> parameters =
                new HashMap<>(specification.workload().parameters());
        parameters.putAll(workflowParameters);
        ExecutionWorkload workload = new ExecutionWorkload(
                specification.workload().type(), specification.workload().payload(),
                parameters, specification.workload().options());
        return new ExecutionSpecification(
                specification.queueId(), specification.accountId(), workload,
                specification.resultMode(), specification.resultPageSize());
    }

    private static TaskInstanceState mapState(ExecutionState state) {
        return switch (state) {
            case QUEUED, DISPATCHING -> TaskInstanceState.QUEUED;
            case RUNNING -> TaskInstanceState.RUNNING;
            case SUCCEEDED -> TaskInstanceState.SUCCEEDED;
            case FAILED, TIMED_OUT -> TaskInstanceState.FAILED;
            case CANCELLED -> TaskInstanceState.CANCELLED;
            case PENDING -> null;
        };
    }

    private WorkflowVersion requireVersion(WorkflowId workflowId, long version) {
        return workflows.findVersion(workflowId, version)
                .orElseThrow(() -> new IllegalArgumentException("workflow version does not exist"));
    }

    private StoredWorkflowRun requireRun(WorkflowInstanceId instanceId) {
        return store.find(Objects.requireNonNull(instanceId, "instanceId must not be null"))
                .orElseThrow(() -> new IllegalArgumentException("workflow instance does not exist"));
    }

    private String nextId(String type) {
        return idGenerator.nextIdValue(IdGenerationRequest.of("workflow-core", type));
    }

    private static String executionKey(WorkflowInstance instance, TaskInstance task) {
        return instance.instanceId().value() + ":" + task.taskKey() + ":" + task.attempt();
    }
}
