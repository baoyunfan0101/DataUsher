package com.datausher.workflow.core;

import com.datausher.execution.api.CancelExecutionRequest;
import com.datausher.execution.api.ExecutionAccountId;
import com.datausher.execution.api.ExecutionCommandService;
import com.datausher.execution.api.ExecutionInstance;
import com.datausher.execution.api.ExecutionInstanceId;
import com.datausher.execution.api.ExecutionOrigin;
import com.datausher.execution.api.ExecutionQuery;
import com.datausher.execution.api.ExecutionQueryService;
import com.datausher.execution.api.ExecutionQueueId;
import com.datausher.execution.api.ExecutionRequest;
import com.datausher.execution.api.ExecutionRequestId;
import com.datausher.execution.api.ExecutionResultMode;
import com.datausher.execution.api.ExecutionSpecification;
import com.datausher.execution.api.ExecutionState;
import com.datausher.execution.api.ExecutionStateChangedEvent;
import com.datausher.execution.api.ExecutionWorkload;
import com.datausher.execution.api.ExecutionWorkloadType;
import com.datausher.execution.api.SubmitExecutionRequest;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.id.core.UuidIdGenerator;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;
import com.datausher.platform.shared.time.core.SystemClock;
import com.datausher.workflow.api.TaskDependency;
import com.datausher.workflow.api.TaskDependencyCondition;
import com.datausher.workflow.api.TaskInstanceState;
import com.datausher.workflow.api.TaskRetryPolicy;
import com.datausher.workflow.api.TriggerWorkflowRequest;
import com.datausher.workflow.api.WorkflowDefinition;
import com.datausher.workflow.api.WorkflowId;
import com.datausher.workflow.api.WorkflowInstanceState;
import com.datausher.workflow.api.WorkflowQueryService;
import com.datausher.workflow.api.WorkflowTaskDefinition;
import com.datausher.workflow.api.WorkflowVersion;
import com.datausher.workflow.api.WorkflowVersionSpec;
import com.datausher.workflow.api.WorkflowTaskAction;
import com.datausher.workflow.api.WorkflowTaskRunReference;
import com.datausher.workflow.api.WorkflowTaskRunReferenceType;
import com.datausher.workflow.api.WorkflowTaskType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultWorkflowRuntimeServiceTest {
    @Test
    void dispatchesExecutionBackedTasksInDependencyOrder() {
        WorkflowId workflowId = new WorkflowId("daily-orders");
        WorkflowVersion version = new WorkflowVersion(
                workflowId, 1,
                new WorkflowVersionSpec(
                        List.of(task("extract"), task("load")),
                        List.of(new TaskDependency(
                                "extract", "load", TaskDependencyCondition.ON_SUCCESS)),
                        Optional.empty(), Map.of()), Instant.EPOCH, "system");
        RecordingExecutions executions = new RecordingExecutions();
        var events = new java.util.ArrayList<com.datausher.platform.shared.event.DomainEvent>();
        var service = new DefaultWorkflowRuntimeService(
                versions(version), new InMemoryWorkflowRuntimeStore(), executions, executions,
                new UuidIdGenerator(), new SystemClock(), events::add);
        RequestContext context = RequestContext.system("request-1", Instant.now());

        var instance = service.trigger(new TriggerWorkflowRequest(
                workflowId, 1, "run-1", Map.of(), context));
        service.dispatchReady(instance.instanceId(), context);

        assertEquals(1, executions.submitted.size());
        assertEquals(TaskInstanceState.WAITING,
                service.listTaskInstances(instance.instanceId()).get(1).state());
        assertEquals(List.of(
                        "workflow.triggered", "workflow.instance-state-changed",
                        "workflow.task-submitted"),
                events.stream().map(com.datausher.platform.shared.event.DomainEvent::eventType).toList());

        service.handleExecutionStateChanged(completed(executions.submitted.get(0), context));
        service.dispatchReady(instance.instanceId(), context);
        assertEquals(2, executions.submitted.size());

        service.handleExecutionStateChanged(completed(executions.submitted.get(1), context));
        assertEquals(WorkflowInstanceState.SUCCEEDED,
                service.findInstance(instance.instanceId()).orElseThrow().state());
    }

    @Test
    void dispatchesRegisteredCustomTaskActions() {
        WorkflowId workflowId = new WorkflowId("custom-workflow");
        WorkflowTaskType customType = new WorkflowTaskType("custom");
        WorkflowTaskDefinition customTask = new WorkflowTaskDefinition(
                "custom", "Custom", new FixtureAction(customType), TaskRetryPolicy.NONE,
                Duration.ofMinutes(1), Map.of());
        WorkflowVersion version = new WorkflowVersion(
                workflowId, 1, new WorkflowVersionSpec(
                        List.of(customTask), List.of(), Optional.empty(), Map.of()),
                Instant.EPOCH, "system");
        WorkflowTaskExecutor executor = new WorkflowTaskExecutor() {
            @Override
            public WorkflowTaskType taskType() {
                return customType;
            }

            @Override
            public WorkflowTaskRunReference dispatch(WorkflowTaskDispatchRequest request) {
                return new WorkflowTaskRunReference(
                        new WorkflowTaskRunReferenceType("custom-run"), "run-1", Map.of());
            }

            @Override
            public void cancel(WorkflowTaskCancelRequest request) {
            }
        };
        var service = new DefaultWorkflowRuntimeService(
                versions(version), new InMemoryWorkflowRuntimeStore(),
                new WorkflowTaskExecutorRegistry(List.of(executor)),
                TaskDependencyConditionRegistry.standard(), TaskRetryStrategyRegistry.standard(),
                new UuidIdGenerator(), new SystemClock(), event -> { });
        RequestContext context = RequestContext.system("request-custom", Instant.now());

        var instance = service.trigger(new TriggerWorkflowRequest(
                workflowId, 1, "custom-run", Map.of(), context));
        service.dispatchReady(instance.instanceId(), context);

        assertEquals("custom-run", service.listTaskInstances(instance.instanceId()).getFirst()
                .runReference().orElseThrow().type().value());
    }

    @Test
    void enforcesTaskTimeoutsAndCancelsUnderlyingRuns() {
        WorkflowId workflowId = new WorkflowId("timeout-workflow");
        WorkflowTaskDefinition task = new WorkflowTaskDefinition(
                "slow", "Slow", specification("slow"), TaskRetryPolicy.NONE,
                Duration.ofSeconds(1), Map.of());
        WorkflowVersion version = new WorkflowVersion(
                workflowId, 1, new WorkflowVersionSpec(
                        List.of(task), List.of(), Optional.empty(), Map.of()),
                Instant.EPOCH, "system");
        RecordingExecutions executions = new RecordingExecutions();
        MutableClock clock = new MutableClock(Instant.EPOCH);
        var service = new DefaultWorkflowRuntimeService(
                versions(version), new InMemoryWorkflowRuntimeStore(), executions, executions,
                new UuidIdGenerator(), clock, event -> { });
        RequestContext context = RequestContext.system("request-timeout", Instant.EPOCH);
        var instance = service.trigger(new TriggerWorkflowRequest(
                workflowId, 1, "timeout-run", Map.of(), context));
        service.dispatchReady(instance.instanceId(), context);

        clock.advance(Duration.ofSeconds(2));
        service.dispatchReady(instance.instanceId(), context);

        assertEquals(TaskInstanceState.TIMED_OUT,
                service.listTaskInstances(instance.instanceId()).getFirst().state());
        assertEquals(WorkflowInstanceState.TIMED_OUT,
                service.findInstance(instance.instanceId()).orElseThrow().state());
        assertEquals(1, executions.cancelled);
    }

    @Test
    void rejectsUnregisteredDependencyConditionsInsteadOfSkippingTasks() {
        WorkflowId workflowId = new WorkflowId("condition-workflow");
        WorkflowVersion version = new WorkflowVersion(
                workflowId, 1, new WorkflowVersionSpec(
                        List.of(task("first"), task("second")),
                        List.of(new TaskDependency(
                                "first", "second", new TaskDependencyCondition("custom"))),
                        Optional.empty(), Map.of()), Instant.EPOCH, "system");
        RecordingExecutions executions = new RecordingExecutions();
        var service = new DefaultWorkflowRuntimeService(
                versions(version), new InMemoryWorkflowRuntimeStore(), executions, executions,
                new UuidIdGenerator(), new SystemClock(), event -> { });
        RequestContext context = RequestContext.system("request-condition", Instant.now());
        var instance = service.trigger(new TriggerWorkflowRequest(
                workflowId, 1, "condition-run", Map.of(), context));
        service.dispatchReady(instance.instanceId(), context);

        assertThrows(IllegalStateException.class, () -> service.handleExecutionStateChanged(
                completed(executions.submitted.getFirst(), context)));
    }

    private static ExecutionStateChangedEvent completed(
            ExecutionRequest request,
            RequestContext context
    ) {
        Instant now = Instant.now();
        ExecutionRequest completed = new ExecutionRequest(
                request.requestId(), request.specification(), request.idempotencyKey(), request.origin(),
                ExecutionState.SUCCEEDED, request.submittedAt(), now, Optional.empty(), request.revision() + 1);
        return new ExecutionStateChangedEvent(
                "event-" + request.requestId().value(), now, context, completed, Optional.empty());
    }

    private static WorkflowTaskDefinition task(String key) {
        return new WorkflowTaskDefinition(
                key, key, specification(key),
                TaskRetryPolicy.NONE, Duration.ofMinutes(10), Map.of());
    }

    private static ExecutionSpecification specification(String payload) {
        return new ExecutionSpecification(
                new ExecutionQueueId("default"), new ExecutionAccountId("local"),
                new ExecutionWorkload(
                        new ExecutionWorkloadType("fixture"), payload, Map.of(), Map.of()),
                ExecutionResultMode.DISCARD, 100);
    }

    private record FixtureAction(WorkflowTaskType taskType) implements WorkflowTaskAction {
    }

    private static final class MutableClock implements com.datausher.platform.shared.time.Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public Instant now() {
            return current;
        }

        @Override
        public java.time.ZoneId zone() {
            return java.time.ZoneId.of("UTC");
        }
    }

    private static WorkflowQueryService versions(WorkflowVersion version) {
        return new WorkflowQueryService() {
            @Override
            public Optional<WorkflowDefinition> findWorkflow(WorkflowId workflowId) {
                return Optional.empty();
            }

            @Override
            public Optional<WorkflowVersion> findVersion(WorkflowId workflowId, long number) {
                return version.workflowId().equals(workflowId) && version.version() == number
                        ? Optional.of(version) : Optional.empty();
            }

            @Override
            public Optional<WorkflowVersion> findLatestVersion(WorkflowId workflowId) {
                return Optional.of(version);
            }

            @Override
            public List<WorkflowVersion> listVersions(WorkflowId workflowId) {
                return List.of(version);
            }
        };
    }

    private static final class RecordingExecutions
            implements ExecutionCommandService, ExecutionQueryService {
        private final List<ExecutionRequest> submitted = new ArrayList<>();
        private final Map<ExecutionRequestId, ExecutionRequest> requests = new HashMap<>();
        private int cancelled;

        @Override
        public ExecutionRequest submit(SubmitExecutionRequest request) {
            ExecutionRequest execution = new ExecutionRequest(
                    new ExecutionRequestId("execution-" + (submitted.size() + 1)),
                    request.specification(), request.idempotencyKey(), request.origin(),
                    ExecutionState.QUEUED, Instant.now(), Instant.now(), Optional.empty(), 1);
            submitted.add(execution);
            requests.put(execution.requestId(), execution);
            return execution;
        }

        @Override
        public ExecutionRequest cancel(CancelExecutionRequest request) {
            cancelled++;
            ExecutionRequest current = requests.get(request.requestId());
            ExecutionRequest cancelled = new ExecutionRequest(
                    current.requestId(), current.specification(), current.idempotencyKey(), current.origin(),
                    ExecutionState.CANCELLED, current.submittedAt(), Instant.now(), Optional.empty(),
                    current.revision() + 1);
            requests.put(cancelled.requestId(), cancelled);
            return cancelled;
        }

        @Override
        public Optional<ExecutionRequest> findRequest(ExecutionRequestId requestId) {
            return Optional.ofNullable(requests.get(requestId));
        }

        @Override
        public Optional<ExecutionInstance> findInstance(ExecutionInstanceId instanceId) {
            return Optional.empty();
        }

        @Override
        public List<ExecutionInstance> listInstances(ExecutionRequestId requestId) {
            return List.of();
        }

        @Override
        public PageResult<ExecutionRequest> search(ExecutionQuery query, PageRequest pageRequest) {
            return new PageResult<>(List.copyOf(requests.values()), requests.size(),
                    pageRequest.page(), pageRequest.size());
        }
    }
}
