package com.datausher.workflow.core;

import com.datausher.execution.api.ExecutionAccountId;
import com.datausher.execution.api.ExecutionQueueId;
import com.datausher.execution.api.ExecutionResultMode;
import com.datausher.execution.api.ExecutionSpecification;
import com.datausher.execution.api.ExecutionWorkload;
import com.datausher.execution.api.ExecutionWorkloadType;
import com.datausher.integration.runtime.api.AdapterOperation;
import com.datausher.integration.runtime.api.IntegrationValue;
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
import com.datausher.integration.scheduler.api.SchedulerTaskDefinition;
import com.datausher.integration.scheduler.api.SchedulerTaskType;
import com.datausher.integration.scheduler.api.WorkflowDefinition;
import com.datausher.integration.scheduler.api.WorkflowRunHandle;
import com.datausher.integration.scheduler.api.WorkflowRunState;
import com.datausher.integration.scheduler.api.WorkflowRunStatus;
import com.datausher.integration.scheduler.api.WorkflowSchedulerAdapter;
import com.datausher.integration.scheduler.api.WorkflowTaskRunPage;
import com.datausher.integration.scheduler.api.WorkflowTrigger;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.time.core.SystemClock;
import com.datausher.platform.shared.id.core.UuidIdGenerator;
import com.datausher.workflow.api.PublishWorkflowRequest;
import com.datausher.workflow.api.AdapterWorkflowTaskAction;
import com.datausher.workflow.api.TaskRetryPolicy;
import com.datausher.workflow.api.WorkflowId;
import com.datausher.workflow.api.WorkflowQueryService;
import com.datausher.workflow.api.WorkflowTaskDefinition;
import com.datausher.workflow.api.WorkflowTaskAction;
import com.datausher.workflow.api.WorkflowTaskType;
import com.datausher.workflow.api.WorkflowRuntimeBinding;
import com.datausher.workflow.api.WorkflowVersion;
import com.datausher.workflow.api.WorkflowVersionSpec;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultWorkflowPublicationServiceTest {
    @Test
    void publishesPortableWorkflowIdempotently() {
        WorkflowId workflowId = new WorkflowId("daily-orders");
        WorkflowVersion version = new WorkflowVersion(
                workflowId, 1, new WorkflowVersionSpec(
                        List.of(task("extract")), List.of(), List.of(),
                        WorkflowRuntimeBinding.schedulerManaged(
                                "scheduler", "binding", Map.of()), Map.of()),
                Instant.EPOCH, "system");
        WorkflowQueryService workflows = versions(version);
        var registry = new InMemoryAdapterRegistry();
        registry.register(new RecordingScheduler());
        var events = new java.util.ArrayList<com.datausher.platform.shared.event.DomainEvent>();
        var service = new DefaultWorkflowPublicationService(
                workflows, new InMemoryWorkflowPublicationStore(), registry,
                new DirectInvocationExecutor(), new SystemClock(), Duration.ofSeconds(30),
                new UuidIdGenerator(), events::add);
        var request = new PublishWorkflowRequest(
                workflowId, 1, "scheduler", "binding", "publish-1",
                RequestContext.system("request-1", Instant.now()));

        var first = service.publish(request);
        var duplicate = service.publish(request);

        assertEquals(first, duplicate);
        assertEquals("external-daily-orders", first.externalWorkflowId());
        assertEquals(List.of("workflow.published"),
                events.stream().map(com.datausher.platform.shared.event.DomainEvent::eventType).toList());
    }

    @Test
    void publishesRegisteredCustomTaskActions() {
        WorkflowId workflowId = new WorkflowId("custom-workflow");
        WorkflowTaskType customType = new WorkflowTaskType("custom");
        WorkflowTaskDefinition customTask = new WorkflowTaskDefinition(
                "custom", "Custom", new FixtureAction(customType), TaskRetryPolicy.NONE,
                Duration.ofMinutes(10), Map.of());
        WorkflowVersion version = new WorkflowVersion(
                workflowId, 1, new WorkflowVersionSpec(
                        List.of(customTask), List.of(), List.of(),
                        WorkflowRuntimeBinding.schedulerManaged(
                                "scheduler", "binding", Map.of()), Map.of()),
                Instant.EPOCH, "system");
        var registry = new InMemoryAdapterRegistry();
        var scheduler = new RecordingScheduler();
        registry.register(scheduler);
        SchedulerTaskDefinitionMapper customMapper = new SchedulerTaskDefinitionMapper() {
            @Override
            public WorkflowTaskType taskType() {
                return customType;
            }

            @Override
            public SchedulerTaskDefinition map(SchedulerTaskMappingRequest request) {
                return new SchedulerTaskDefinition(
                        request.taskDefinition().taskKey(), new SchedulerTaskType("custom"),
                        "custom-payload", Map.of(), Map.of());
            }
        };
        var service = new DefaultWorkflowPublicationService(
                versions(version), new InMemoryWorkflowPublicationStore(), registry,
                new DirectInvocationExecutor(), new SchedulerTaskDefinitionMapperRegistry(
                List.of(new ExecutionSchedulerTaskDefinitionMapper(), customMapper)),
                new SystemClock(), Duration.ofSeconds(30), new UuidIdGenerator(), event -> { });

        service.publish(new PublishWorkflowRequest(
                workflowId, 1, "scheduler", "binding", "publish-custom",
                RequestContext.system("request-custom", Instant.now())));

        assertEquals("custom", scheduler.lastDefinition.tasks().getFirst().taskType().value());
    }

    @Test
    void mapsAdapterTasksToPortableSchedulerDefinitions() {
        WorkflowId workflowId = new WorkflowId("adapter-workflow");
        AdapterOperation operation = AdapterOperation.of(
                "visualization.dataset.bind", "visualization.dataset.bind", true);
        WorkflowTaskDefinition adapterTask = new WorkflowTaskDefinition(
                "publish-dataset",
                "Publish dataset",
                new AdapterWorkflowTaskAction(
                        "superset", "dashboard-prod", operation,
                        Map.of("datasetKey", new IntegrationValue.TextValue("daily-sales")),
                        "daily-sales-dataset"),
                TaskRetryPolicy.NONE, Duration.ofMinutes(10), Map.of());
        WorkflowVersion version = new WorkflowVersion(
                workflowId, 1, new WorkflowVersionSpec(
                        List.of(adapterTask), List.of(), List.of(),
                        WorkflowRuntimeBinding.schedulerManaged(
                                "scheduler", "binding", Map.of()), Map.of()),
                Instant.EPOCH, "system");
        var registry = new InMemoryAdapterRegistry();
        var scheduler = new RecordingScheduler();
        registry.register(scheduler);
        var service = new DefaultWorkflowPublicationService(
                versions(version), new InMemoryWorkflowPublicationStore(), registry,
                new DirectInvocationExecutor(), new SystemClock(), Duration.ofSeconds(30),
                new UuidIdGenerator(), event -> { });

        service.publish(new PublishWorkflowRequest(
                workflowId, 1, "scheduler", "binding", "publish-adapter",
                RequestContext.system("request-adapter", Instant.now())));

        SchedulerTaskDefinition mapped = scheduler.lastDefinition.tasks().getFirst();
        assertEquals(SchedulerTaskType.PLATFORM_ADAPTER, mapped.taskType());
        assertEquals("superset", mapped.options().get("adapterId"));
        assertEquals("dashboard-prod", mapped.options().get("bindingId"));
        assertEquals("visualization.dataset.bind", mapped.options().get("operation"));
        assertEquals("daily-sales-dataset", mapped.options().get("idempotencyKey"));
    }

    private static WorkflowTaskDefinition task(String key) {
        return new WorkflowTaskDefinition(
                key, "Extract",
                new ExecutionSpecification(
                        new ExecutionQueueId("default"), new ExecutionAccountId("local"),
                        new ExecutionWorkload(new ExecutionWorkloadType("fixture"), key, Map.of(), Map.of()),
                        ExecutionResultMode.DISCARD, 100),
                TaskRetryPolicy.NONE, Duration.ofMinutes(10), Map.of());
    }

    private static WorkflowQueryService versions(WorkflowVersion version) {
        return new WorkflowQueryService() {
            @Override
            public Optional<com.datausher.workflow.api.WorkflowDefinition> findWorkflow(WorkflowId workflowId) {
                return Optional.empty();
            }

            @Override
            public Optional<WorkflowVersion> findVersion(WorkflowId workflowId, long number) {
                return version.workflowId().equals(workflowId) && version.version() == number
                        ? Optional.of(version) : Optional.empty();
            }

            @Override
            public Optional<WorkflowVersion> findLatestVersion(WorkflowId workflowId) {
                return version.workflowId().equals(workflowId) ? Optional.of(version) : Optional.empty();
            }

            @Override
            public List<WorkflowVersion> listVersions(WorkflowId workflowId) {
                return version.workflowId().equals(workflowId) ? List.of(version) : List.of();
            }
        };
    }

    private record FixtureAction(WorkflowTaskType taskType) implements WorkflowTaskAction {
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
                        AdapterCapability.of(SchedulerCapabilities.WORKFLOW_PUBLICATION),
                        AdapterCapability.of(SchedulerCapabilities.WORKFLOW_EXECUTION),
                        AdapterCapability.of(SchedulerCapabilities.TASK_OBSERVATION)), Map.of());
        private WorkflowDefinition lastDefinition;

        @Override
        public PublishedWorkflow publish(AdapterRequestContext context, WorkflowDefinition definition) {
            lastDefinition = definition;
            return new PublishedWorkflow(
                    DESCRIPTOR.adapterId(), definition.bindingId(), definition.workflowId(),
                    definition.idempotencyKey(), "external-" + definition.workflowId(), definition.revision());
        }

        @Override
        public void unpublish(AdapterRequestContext context, PublishedWorkflow workflow) {
        }

        @Override
        public WorkflowRunHandle trigger(AdapterRequestContext context, WorkflowTrigger trigger) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WorkflowRunStatus status(AdapterRequestContext context, WorkflowRunHandle handle) {
            return new WorkflowRunStatus(handle, WorkflowRunState.QUEUED, Instant.now(), "", Map.of());
        }

        @Override
        public WorkflowTaskRunPage readTaskRuns(
                AdapterRequestContext context, WorkflowRunHandle handle, String cursor, int limit
        ) {
            return new WorkflowTaskRunPage(handle, List.of(), "", true);
        }

        @Override
        public void cancel(AdapterRequestContext context, WorkflowRunHandle handle) {
        }

        @Override
        public AdapterDescriptor descriptor() {
            return DESCRIPTOR;
        }

        @Override
        public AdapterHealth checkHealth() {
            return new AdapterHealth(
                    DESCRIPTOR.adapterId(), AdapterHealthStatus.UP, Instant.now(), "", Map.of());
        }
    }
}
