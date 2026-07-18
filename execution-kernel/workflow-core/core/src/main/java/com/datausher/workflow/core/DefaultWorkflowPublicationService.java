package com.datausher.workflow.core;

import com.datausher.integration.runtime.api.AdapterInvocationExecutor;
import com.datausher.integration.runtime.api.AdapterRegistry;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.scheduler.api.PublishedWorkflow;
import com.datausher.integration.scheduler.api.SchedulerDependencyCondition;
import com.datausher.integration.scheduler.api.SchedulerSchedule;
import com.datausher.integration.scheduler.api.SchedulerScheduleType;
import com.datausher.integration.scheduler.api.SchedulerScheduleStatus;
import com.datausher.integration.scheduler.api.SchedulerTaskDependency;
import com.datausher.integration.scheduler.api.WorkflowSchedulerAdapter;
import com.datausher.platform.shared.time.Clock;
import com.datausher.platform.shared.event.DomainEventPublisher;
import com.datausher.platform.shared.id.IdGenerationRequest;
import com.datausher.platform.shared.id.IdGenerator;
import com.datausher.workflow.api.PublishWorkflowRequest;
import com.datausher.workflow.api.TaskDependencyCondition;
import com.datausher.workflow.api.WorkflowId;
import com.datausher.workflow.api.WorkflowPublication;
import com.datausher.workflow.api.WorkflowPublicationService;
import com.datausher.workflow.api.WorkflowPublishedEvent;
import com.datausher.workflow.api.WorkflowQueryService;
import com.datausher.workflow.api.WorkflowSchedule;
import com.datausher.workflow.api.WorkflowRuntimeType;
import com.datausher.workflow.api.WorkflowVersion;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DefaultWorkflowPublicationService implements WorkflowPublicationService {
    private final WorkflowQueryService workflows;
    private final WorkflowPublicationStore store;
    private final AdapterRegistry adapters;
    private final AdapterInvocationExecutor invocationExecutor;
    private final SchedulerTaskDefinitionMapperRegistry taskMappers;
    private final Clock clock;
    private final Duration adapterTimeout;
    private final IdGenerator idGenerator;
    private final DomainEventPublisher eventPublisher;

    public DefaultWorkflowPublicationService(
            WorkflowQueryService workflows,
            WorkflowPublicationStore store,
            AdapterRegistry adapters,
            AdapterInvocationExecutor invocationExecutor,
            SchedulerTaskDefinitionMapperRegistry taskMappers,
            Clock clock,
            Duration adapterTimeout,
            IdGenerator idGenerator,
            DomainEventPublisher eventPublisher
    ) {
        this.workflows = Objects.requireNonNull(workflows, "workflows must not be null");
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.adapters = Objects.requireNonNull(adapters, "adapters must not be null");
        this.invocationExecutor = Objects.requireNonNull(
                invocationExecutor, "invocationExecutor must not be null");
        this.taskMappers = Objects.requireNonNull(taskMappers, "taskMappers must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.adapterTimeout = Objects.requireNonNull(adapterTimeout, "adapterTimeout must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        if (adapterTimeout.isZero() || adapterTimeout.isNegative()) {
            throw new IllegalArgumentException("adapterTimeout must be positive");
        }
    }

    public DefaultWorkflowPublicationService(
            WorkflowQueryService workflows,
            WorkflowPublicationStore store,
            AdapterRegistry adapters,
            AdapterInvocationExecutor invocationExecutor,
            Clock clock,
            Duration adapterTimeout,
            IdGenerator idGenerator,
            DomainEventPublisher eventPublisher
    ) {
        this(workflows, store, adapters, invocationExecutor,
                new SchedulerTaskDefinitionMapperRegistry(List.of(
                        new ExecutionSchedulerTaskDefinitionMapper())),
                clock, adapterTimeout, idGenerator, eventPublisher);
    }

    @Override
    public WorkflowPublication publish(PublishWorkflowRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Optional<WorkflowPublication> existing = store.find(request.workflowId(), request.version());
        if (existing.isPresent()) {
            return requireSamePublication(existing.orElseThrow(), request);
        }
        WorkflowVersion version = workflows.findVersion(request.workflowId(), request.version())
                .orElseThrow(() -> new IllegalArgumentException("workflow version does not exist"));
        if (!version.specification().runtimeBinding().runtimeType()
                .equals(WorkflowRuntimeType.SCHEDULER_MANAGED)) {
            throw new IllegalStateException("only scheduler-managed workflows can be published");
        }
        if (!version.specification().runtimeBinding().adapterId().orElseThrow().equals(request.adapterId())
                || !version.specification().runtimeBinding().bindingId().orElseThrow()
                .equals(request.bindingId())) {
            throw new IllegalArgumentException("publication must match workflow runtime binding");
        }
        WorkflowSchedulerAdapter adapter = adapters.find(
                        request.adapterId(), WorkflowSchedulerAdapter.class)
                .orElseThrow(() -> new IllegalArgumentException(
                        "workflow scheduler adapter does not exist: " + request.adapterId()));
        var adapterContext = new AdapterRequestContext(
                request.requestContext().requestId(), clock.now().plus(adapterTimeout), Map.of());
        var definition = toSchedulerDefinition(version, request);
        PublishedWorkflow published = invocationExecutor.execute(
                adapterContext, adapter, "publish", () -> adapter.publish(adapterContext, definition));
        validatePublished(published, definition, adapter);
        WorkflowPublication publication = new WorkflowPublication(
                request.workflowId(), request.version(), published.adapterId(), published.bindingId(),
                request.idempotencyKey(), published.externalWorkflowId(), published.revision(),
                clock.now(), request.requestContext().actor().actorId());
        WorkflowPublicationCreateResult creation = store.createOrFind(publication);
        WorkflowPublication stored = requireSamePublication(creation.publication(), request);
        if (creation.created()) {
            eventPublisher.publish(new WorkflowPublishedEvent(
                    idGenerator.nextIdValue(IdGenerationRequest.of("workflow", "domain-event")),
                    stored.publishedAt(), request.requestContext(), stored));
        }
        return stored;
    }

    @Override
    public Optional<WorkflowPublication> findPublication(WorkflowId workflowId, long version) {
        return store.find(Objects.requireNonNull(workflowId, "workflowId must not be null"), version);
    }

    private com.datausher.integration.scheduler.api.WorkflowDefinition toSchedulerDefinition(
            WorkflowVersion version,
            PublishWorkflowRequest request
    ) {
        var tasks = version.specification().tasks().stream()
                .map(task -> taskMappers.require(task.action().taskType())
                        .map(new SchedulerTaskMappingRequest(version, task)))
                .toList();
        var dependencies = version.specification().dependencies().stream()
                .map(dependency -> new SchedulerTaskDependency(
                        dependency.upstreamTaskKey(), dependency.downstreamTaskKey(),
                        toSchedulerCondition(dependency.condition())))
                .toList();
        var schedules = version.specification().schedules().stream()
                .map(this::toSchedulerSchedule).toList();
        return new com.datausher.integration.scheduler.api.WorkflowDefinition(
                request.bindingId(), version.workflowId().value(), version.version(),
                request.idempotencyKey(), tasks, dependencies, schedules,
                version.specification().attributes());
    }

    private SchedulerSchedule toSchedulerSchedule(WorkflowSchedule schedule) {
        return new SchedulerSchedule(
                schedule.scheduleId().value(), new SchedulerScheduleType(schedule.type().value()),
                schedule.expression(), schedule.zoneId(),
                SchedulerScheduleStatus.valueOf(schedule.status().name()), schedule.options());
    }

    private static SchedulerDependencyCondition toSchedulerCondition(TaskDependencyCondition condition) {
        return new SchedulerDependencyCondition(condition.value());
    }

    private static WorkflowPublication requireSamePublication(
            WorkflowPublication publication,
            PublishWorkflowRequest request
    ) {
        if (!publication.workflowId().equals(request.workflowId())
                || publication.version() != request.version()
                || !publication.adapterId().equals(request.adapterId())
                || !publication.bindingId().equals(request.bindingId())
                || !publication.idempotencyKey().equals(request.idempotencyKey())) {
            throw new IllegalStateException("workflow publication identity conflicts with existing publication");
        }
        return publication;
    }

    private static void validatePublished(
            PublishedWorkflow published,
            com.datausher.integration.scheduler.api.WorkflowDefinition definition,
            WorkflowSchedulerAdapter adapter
    ) {
        Objects.requireNonNull(published, "scheduler adapter returned null publication");
        if (!published.adapterId().equals(adapter.descriptor().adapterId())
                || !published.bindingId().equals(definition.bindingId())
                || !published.workflowId().equals(definition.workflowId())
                || !published.idempotencyKey().equals(definition.idempotencyKey())
                || published.revision() != definition.revision()) {
            throw new IllegalStateException("scheduler adapter returned mismatched publication identity");
        }
    }
}
