package com.datausher.workflow.core;

import com.datausher.governance.resource.api.RegisteredResource;
import com.datausher.governance.resource.api.ResourceLifecycle;
import com.datausher.governance.resource.api.ResourceQueryService;
import com.datausher.platform.audit.api.AuditOutcome;
import com.datausher.platform.audit.api.AuditRecordRequest;
import com.datausher.platform.audit.api.AuditTarget;
import com.datausher.platform.audit.api.AuditedCommand;
import com.datausher.platform.audit.api.AuditedCommandExecutor;
import com.datausher.platform.shared.time.Clock;
import com.datausher.workflow.api.CreateWorkflowRequest;
import com.datausher.workflow.api.CreateWorkflowVersionRequest;
import com.datausher.workflow.api.WorkflowCommandService;
import com.datausher.workflow.api.WorkflowDefinition;
import com.datausher.workflow.api.WorkflowId;
import com.datausher.workflow.api.WorkflowQueryService;
import com.datausher.workflow.api.WorkflowVersion;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DefaultWorkflowService implements WorkflowCommandService, WorkflowQueryService {
    private final WorkflowStore store;
    private final ResourceQueryService resources;
    private final Clock clock;
    private final AuditedCommandExecutor commandExecutor;

    public DefaultWorkflowService(
            WorkflowStore store,
            ResourceQueryService resources,
            Clock clock,
            AuditedCommandExecutor commandExecutor
    ) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.resources = Objects.requireNonNull(resources, "resources must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor must not be null");
    }

    @Override
    public WorkflowDefinition create(CreateWorkflowRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        requireActiveResource(request.resourceRef());
        Instant now = clock.now();
        WorkflowDefinition workflow = new WorkflowDefinition(
                request.workflowId(), request.resourceRef(), request.displayName(), 0, 1,
                now, now, request.requestContext().actor().actorId(), request.attributes());
        return commandExecutor.execute(new AuditedCommand<>() {
            @Override
            public WorkflowDefinition execute() {
                store.createWorkflow(workflow);
                return workflow;
            }

            @Override
            public AuditRecordRequest audit(WorkflowDefinition result) {
                return auditRequest(request.requestContext(), "workflow.create", result,
                        Map.of("resource", result.resourceRef().canonicalValue()));
            }

            @Override
            public void rollback(WorkflowDefinition result, RuntimeException cause) {
                store.deleteWorkflow(result);
            }
        });
    }

    @Override
    public WorkflowVersion createVersion(CreateWorkflowVersionRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        WorkflowDefinition current = store.findWorkflow(request.workflowId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "workflow does not exist: " + request.workflowId()));
        if (current.revision() != request.expectedRevision()) {
            throw new IllegalStateException("workflow revision does not match expectedRevision");
        }
        long versionNumber = current.latestVersion() + 1;
        Instant now = clock.now();
        WorkflowVersion version = new WorkflowVersion(
                current.workflowId(), versionNumber, request.specification(), now,
                request.requestContext().actor().actorId());
        WorkflowDefinition updated = new WorkflowDefinition(
                current.workflowId(), current.resourceRef(), current.displayName(),
                versionNumber, current.revision() + 1, current.createdAt(), now,
                current.createdBy(), current.attributes());
        return commandExecutor.execute(new AuditedCommand<>() {
            @Override
            public WorkflowVersion execute() {
                store.createVersion(current, updated, version);
                return version;
            }

            @Override
            public AuditRecordRequest audit(WorkflowVersion result) {
                return auditRequest(request.requestContext(), "workflow.version.create", updated,
                        Map.of("version", Long.toString(result.version())));
            }

            @Override
            public void rollback(WorkflowVersion result, RuntimeException cause) {
                store.deleteVersion(updated, current, result);
            }
        });
    }

    @Override
    public Optional<WorkflowDefinition> findWorkflow(WorkflowId workflowId) {
        return store.findWorkflow(Objects.requireNonNull(workflowId, "workflowId must not be null"));
    }

    @Override
    public Optional<WorkflowVersion> findVersion(WorkflowId workflowId, long version) {
        return store.findVersion(Objects.requireNonNull(workflowId, "workflowId must not be null"), version);
    }

    @Override
    public Optional<WorkflowVersion> findLatestVersion(WorkflowId workflowId) {
        WorkflowDefinition workflow = store.findWorkflow(
                Objects.requireNonNull(workflowId, "workflowId must not be null")).orElse(null);
        return workflow == null || workflow.latestVersion() == 0
                ? Optional.empty() : store.findVersion(workflowId, workflow.latestVersion());
    }

    @Override
    public List<WorkflowVersion> listVersions(WorkflowId workflowId) {
        return store.listVersions(Objects.requireNonNull(workflowId, "workflowId must not be null"));
    }

    private void requireActiveResource(com.datausher.governance.resource.api.ResourceRef resourceRef) {
        RegisteredResource resource = resources.find(resourceRef)
                .orElseThrow(() -> new IllegalArgumentException("resource does not exist: " + resourceRef));
        if (resource.lifecycle() != ResourceLifecycle.ACTIVE) {
            throw new IllegalStateException("resource is not active: " + resourceRef);
        }
    }

    private static AuditRecordRequest auditRequest(
            com.datausher.platform.shared.context.RequestContext context,
            String action,
            WorkflowDefinition workflow,
            Map<String, String> details
    ) {
        return new AuditRecordRequest(
                context, "workflow-core", action,
                AuditTarget.global("workflow", workflow.workflowId().value()),
                AuditOutcome.SUCCEEDED, details);
    }
}
