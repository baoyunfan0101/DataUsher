package com.datausher.workflow.core;

import com.datausher.execution.api.ExecutionValue;
import com.datausher.integration.runtime.api.AdapterCapability;
import com.datausher.integration.runtime.api.AdapterDescriptor;
import com.datausher.integration.runtime.api.AdapterHealth;
import com.datausher.integration.runtime.api.AdapterHealthStatus;
import com.datausher.integration.runtime.api.AdapterInvocationExecutor;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.AdapterType;
import com.datausher.integration.runtime.api.IntegrationAdapter;
import com.datausher.integration.runtime.core.InMemoryAdapterRegistry;
import com.datausher.integration.scheduler.api.PublishedWorkflow;
import com.datausher.integration.scheduler.api.SchedulerCapabilities;
import com.datausher.integration.scheduler.api.WorkflowDefinition;
import com.datausher.integration.scheduler.api.WorkflowRunHandle;
import com.datausher.integration.scheduler.api.WorkflowRunState;
import com.datausher.integration.scheduler.api.WorkflowRunStatus;
import com.datausher.integration.scheduler.api.WorkflowSchedulerAdapter;
import com.datausher.integration.scheduler.api.WorkflowTaskRunPage;
import com.datausher.integration.scheduler.api.WorkflowTaskRunStatus;
import com.datausher.integration.scheduler.api.WorkflowTrigger;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.time.core.SystemClock;
import com.datausher.workflow.api.PublishWorkflowRequest;
import com.datausher.workflow.api.TaskInstanceState;
import com.datausher.workflow.api.WorkflowId;
import com.datausher.workflow.api.WorkflowInstance;
import com.datausher.workflow.api.WorkflowInstanceId;
import com.datausher.workflow.api.WorkflowInstanceState;
import com.datausher.workflow.api.WorkflowPublication;
import com.datausher.workflow.api.WorkflowPublicationService;
import com.datausher.workflow.api.WorkflowRuntimeBinding;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultSchedulerManagedWorkflowGatewayTest {
    @Test
    void triggersObservesAndCancelsThroughTheBoundAdapter() {
        WorkflowId workflowId = new WorkflowId("external-workflow");
        WorkflowPublication publication = new WorkflowPublication(
                workflowId, 1, "scheduler", "binding", "publication-key",
                "published-workflow", 1, Instant.EPOCH, "system");
        var scheduler = new RecordingScheduler();
        var adapters = new InMemoryAdapterRegistry();
        adapters.register(scheduler);
        var gateway = new DefaultSchedulerManagedWorkflowGateway(
                publications(publication), adapters, new DirectInvocationExecutor(),
                new SystemClock(), Duration.ofSeconds(30), 100, 10);
        WorkflowInstance pending = new WorkflowInstance(
                new WorkflowInstanceId("instance-1"), workflowId, 1,
                WorkflowRuntimeBinding.schedulerManaged("scheduler", "binding", Map.of()),
                Optional.empty(), "run-key",
                Map.of("region", new ExecutionValue.TextValue("east")),
                WorkflowInstanceState.PENDING, Instant.EPOCH, Instant.EPOCH,
                Optional.empty(), 1);
        RequestContext context = RequestContext.system("request-1", Instant.now());

        var reference = gateway.trigger(pending, context);
        WorkflowInstance running = new WorkflowInstance(
                pending.instanceId(), pending.workflowId(), pending.workflowVersion(),
                pending.runtimeBinding(), Optional.of(reference), pending.idempotencyKey(),
                pending.parameters(), WorkflowInstanceState.RUNNING, pending.createdAt(),
                Instant.now(), Optional.empty(), 2);
        var observation = gateway.observe(running, context);
        gateway.cancel(running, context);

        assertEquals("external-run", reference.externalRunId());
        assertEquals("east", ((com.datausher.integration.runtime.api.IntegrationValue.TextValue)
                scheduler.trigger.parameters().get("region")).value());
        assertEquals(WorkflowInstanceState.FAILED, observation.state().orElseThrow());
        assertEquals(TaskInstanceState.FAILED, observation.tasks().getFirst().state());
        assertEquals("engine-failure",
                observation.tasks().getFirst().failureCode().orElseThrow());
        assertEquals(1, scheduler.cancelled);
    }

    private static WorkflowPublicationService publications(WorkflowPublication publication) {
        return new WorkflowPublicationService() {
            @Override
            public WorkflowPublication publish(PublishWorkflowRequest request) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<WorkflowPublication> findPublication(
                    WorkflowId workflowId,
                    long version
            ) {
                return publication.workflowId().equals(workflowId)
                        && publication.version() == version
                        ? Optional.of(publication) : Optional.empty();
            }
        };
    }

    private static final class DirectInvocationExecutor implements AdapterInvocationExecutor {
        @Override
        public <T> T execute(
                AdapterRequestContext context,
                IntegrationAdapter adapter,
                String operation,
                Supplier<T> invocation
        ) {
            return invocation.get();
        }
    }

    private static final class RecordingScheduler implements WorkflowSchedulerAdapter {
        private static final AdapterDescriptor DESCRIPTOR = new AdapterDescriptor(
                "scheduler", AdapterType.WORKFLOW_SCHEDULER, "1.0.0",
                Set.of(
                        AdapterCapability.of(SchedulerCapabilities.WORKFLOW_EXECUTION),
                        AdapterCapability.of(SchedulerCapabilities.TASK_OBSERVATION)), Map.of());
        private WorkflowTrigger trigger;
        private int cancelled;

        @Override
        public PublishedWorkflow publish(
                AdapterRequestContext context,
                WorkflowDefinition definition
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unpublish(AdapterRequestContext context, PublishedWorkflow workflow) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WorkflowRunHandle trigger(
                AdapterRequestContext context,
                WorkflowTrigger trigger
        ) {
            this.trigger = trigger;
            return handle(trigger.idempotencyKey());
        }

        @Override
        public WorkflowRunStatus status(
                AdapterRequestContext context,
                WorkflowRunHandle handle
        ) {
            return new WorkflowRunStatus(
                    handle, WorkflowRunState.FAILED, Instant.now(), "", Map.of());
        }

        @Override
        public WorkflowTaskRunPage readTaskRuns(
                AdapterRequestContext context,
                WorkflowRunHandle handle,
                String cursor,
                int limit
        ) {
            Instant now = Instant.now();
            return new WorkflowTaskRunPage(
                    handle, List.of(new WorkflowTaskRunStatus(
                    "extract", 1, WorkflowRunState.FAILED, Optional.of(Instant.EPOCH),
                    Optional.of(now), "", Map.of("failureCode", "engine-failure"))),
                    "", true);
        }

        @Override
        public void cancel(AdapterRequestContext context, WorkflowRunHandle handle) {
            cancelled++;
        }

        @Override
        public AdapterDescriptor descriptor() {
            return DESCRIPTOR;
        }

        @Override
        public AdapterHealth checkHealth() {
            return new AdapterHealth(
                    DESCRIPTOR.adapterId(), AdapterHealthStatus.UP,
                    Instant.now(), "", Map.of());
        }

        private static WorkflowRunHandle handle(String idempotencyKey) {
            return new WorkflowRunHandle(
                    "scheduler", "binding", "external-workflow",
                    idempotencyKey, "external-run");
        }
    }
}
