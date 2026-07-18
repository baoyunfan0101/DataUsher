package com.datausher.development.core;

import com.datausher.development.api.RequestScriptPublication;
import com.datausher.development.api.ScriptPublication;
import com.datausher.development.api.ScriptPublicationId;
import com.datausher.development.api.ScriptPublicationService;
import com.datausher.development.api.ScriptPublicationState;
import com.datausher.development.api.ScriptPublicationStateChangedEvent;
import com.datausher.development.api.ScriptQueryService;
import com.datausher.development.api.ScriptVersion;
import com.datausher.governance.approval.api.ApprovalCallbackHandler;
import com.datausher.governance.approval.api.ApprovalCallbackInvocation;
import com.datausher.governance.approval.api.ApprovalCallbackRef;
import com.datausher.governance.approval.api.ApprovalCallbackType;
import com.datausher.governance.approval.api.ApprovalCommandService;
import com.datausher.governance.approval.api.ApprovalRequest;
import com.datausher.governance.approval.api.ApprovalRequestStatus;
import com.datausher.governance.approval.api.SubmitApprovalRequest;
import com.datausher.platform.shared.id.IdGenerationRequest;
import com.datausher.platform.shared.id.IdGenerator;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.concurrent.RevisionConflictException;
import com.datausher.platform.shared.event.DomainEventPublisher;
import com.datausher.platform.shared.time.Clock;
import com.datausher.workflow.api.CreateWorkflowVersionRequest;
import com.datausher.workflow.api.WorkflowCommandService;
import com.datausher.workflow.api.WorkflowDefinition;
import com.datausher.workflow.api.WorkflowQueryService;
import com.datausher.workflow.api.WorkflowTaskDefinition;
import com.datausher.workflow.api.WorkflowVersion;
import com.datausher.workflow.api.WorkflowVersionSpec;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DefaultScriptPublicationService
        implements ScriptPublicationService, ScriptPublicationWorker, ApprovalCallbackHandler {
    public static final ApprovalCallbackType CALLBACK_TYPE =
            new ApprovalCallbackType("development.script-publication");
    private static final String PUBLICATION_ATTRIBUTE = "development.publication-id";
    private static final String SCRIPT_ID_ATTRIBUTE = "development.script-id";
    private static final String SCRIPT_VERSION_ATTRIBUTE = "development.script-version";

    private final ScriptQueryService scripts;
    private final WorkflowCommandService workflowCommands;
    private final WorkflowQueryService workflows;
    private final ApprovalCommandService approvals;
    private final ScriptPublicationStore store;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final DomainEventPublisher eventPublisher;

    public DefaultScriptPublicationService(
            ScriptQueryService scripts,
            WorkflowCommandService workflowCommands,
            WorkflowQueryService workflows,
            ApprovalCommandService approvals,
            ScriptPublicationStore store,
            IdGenerator idGenerator,
            Clock clock,
            DomainEventPublisher eventPublisher
    ) {
        this.scripts = Objects.requireNonNull(scripts, "scripts must not be null");
        this.workflowCommands = Objects.requireNonNull(
                workflowCommands, "workflowCommands must not be null");
        this.workflows = Objects.requireNonNull(workflows, "workflows must not be null");
        this.approvals = Objects.requireNonNull(approvals, "approvals must not be null");
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    @Override
    public ScriptPublication request(RequestScriptPublication request) {
        Objects.requireNonNull(request, "request must not be null");
        StoredScriptPublication existing = store.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existing != null) {
            requireSameRequest(existing, request);
            return existing.publication().state() == ScriptPublicationState.REQUESTED
                    ? submitApproval(existing.publication().publicationId()) : existing.publication();
        }
        requireScriptVersion(request);
        WorkflowDefinition workflow = requireWorkflowAtBase(request);
        requireTargetTask(request);
        Instant now = clock.now();
        ScriptPublication publication = new ScriptPublication(
                new ScriptPublicationId(idGenerator.nextIdValue(
                        IdGenerationRequest.of("development", "script-publication"))),
                request.scriptId(), request.scriptVersion(), request.workflowId(),
                request.baseWorkflowVersion(), request.taskKey(), request.idempotencyKey(),
                ScriptPublicationState.REQUESTED, Optional.empty(), Optional.empty(),
                Optional.empty(), now, now, 1);
        StoredScriptPublication proposed = new StoredScriptPublication(
                publication, request.approvalTemplateKey(), request.requestedBy(),
                request.attributes(), request.requestContext());
        ScriptPublicationCreateResult creation = store.createOrFind(proposed);
        requireSameRequest(creation.publication(), request);
        if (!creation.created()) {
            ScriptPublication current = creation.publication().publication();
            return current.state() == ScriptPublicationState.REQUESTED
                    ? submitApproval(current.publicationId()) : current;
        }
        if (!workflow.workflowId().equals(request.workflowId())) {
            throw new IllegalStateException("workflow identity changed during publication request");
        }
        publishStateChange(Optional.empty(), publication, request.requestContext(), now);
        return submitApproval(publication.publicationId());
    }

    @Override
    public ScriptPublication submitApproval(ScriptPublicationId publicationId) {
        StoredScriptPublication stored = requireStored(publicationId);
        ScriptPublication publication = stored.publication();
        if (publication.state() != ScriptPublicationState.REQUESTED) {
            return publication;
        }
        WorkflowDefinition workflow = workflows.findWorkflow(publication.workflowId())
                .orElseThrow(() -> new IllegalStateException("workflow no longer exists"));
        Map<String, String> attributes = new HashMap<>(stored.approvalAttributes());
        attributes.put(PUBLICATION_ATTRIBUTE, publication.publicationId().value());
        attributes.put(SCRIPT_ID_ATTRIBUTE, publication.scriptId().value());
        attributes.put(SCRIPT_VERSION_ATTRIBUTE, Long.toString(publication.scriptVersion()));
        ApprovalRequest approval = approvals.submit(new SubmitApprovalRequest(
                stored.approvalTemplateKey(),
                "Publish script " + publication.scriptId().value() + "@" + publication.scriptVersion(),
                workflow.resourceRef(), stored.requestedBy(),
                new ApprovalCallbackRef(CALLBACK_TYPE, publication.publicationId().value(), Map.of()),
                "script-publication:" + publication.publicationId().value(), attributes,
                stored.requestContext()));
        Instant updatedAt = clock.now();
        ScriptPublicationTransitionResult transition = store.attachApproval(
                publication, approval.requestId(), updatedAt);
        if (transition.changed()) {
            publishStateChange(
                    Optional.of(publication.state()), transition.publication(),
                    stored.requestContext(), updatedAt);
        }
        return transition.publication();
    }

    @Override
    public Optional<ScriptPublication> findPublication(ScriptPublicationId publicationId) {
        return store.find(Objects.requireNonNull(publicationId, "publicationId must not be null"))
                .map(StoredScriptPublication::publication);
    }

    @Override
    public ApprovalCallbackType callbackType() {
        return CALLBACK_TYPE;
    }

    @Override
    public void handle(ApprovalCallbackInvocation invocation) {
        Objects.requireNonNull(invocation, "invocation must not be null");
        if (!CALLBACK_TYPE.equals(invocation.callbackType())) {
            throw new IllegalArgumentException("unsupported callback type: " + invocation.callbackType());
        }
        StoredScriptPublication stored = requireStored(
                new ScriptPublicationId(invocation.correlationKey()));
        ScriptPublication publication = stored.publication();
        if (publication.state().terminal()) {
            return;
        }
        if (!publication.approvalRequestId().orElseThrow().equals(invocation.approvalRequestId())) {
            throw new IllegalArgumentException("approval request does not match publication");
        }
        WorkflowDefinition workflow = workflows.findWorkflow(publication.workflowId())
                .orElseThrow(() -> new IllegalStateException("workflow no longer exists"));
        if (!workflow.resourceRef().equals(invocation.targetResource())) {
            throw new IllegalArgumentException("approval target does not match workflow");
        }
        if (invocation.approvalStatus() == ApprovalRequestStatus.REJECTED) {
            complete(publication, ScriptPublicationState.REJECTED,
                    Optional.empty(), Optional.empty(), invocation.requestContext());
            return;
        }
        if (invocation.approvalStatus() == ApprovalRequestStatus.CANCELLED) {
            complete(publication, ScriptPublicationState.CANCELLED,
                    Optional.empty(), Optional.empty(), invocation.requestContext());
            return;
        }
        publishApproved(publication, workflow, invocation);
    }

    private void publishApproved(
            ScriptPublication publication,
            WorkflowDefinition workflow,
            ApprovalCallbackInvocation invocation
    ) {
        Optional<WorkflowVersion> latest = workflows.findLatestVersion(publication.workflowId());
        if (latest.filter(version -> publicationMarker(version).equals(publication.publicationId().value()))
                .isPresent()) {
            completePublished(publication, latest.orElseThrow().version(), invocation.requestContext());
            return;
        }
        if (workflow.latestVersion() != publication.baseWorkflowVersion()) {
            completeConflict(publication, "workflow-version-changed", invocation.requestContext());
            return;
        }
        WorkflowVersion base = workflows.findVersion(
                        publication.workflowId(), publication.baseWorkflowVersion())
                .orElseThrow(() -> new IllegalStateException("base workflow version no longer exists"));
        ScriptVersion script = scripts.findVersion(publication.scriptId(), publication.scriptVersion())
                .orElseThrow(() -> new IllegalStateException("script version no longer exists"));
        WorkflowVersionSpec specification = replaceTask(base, publication, script);
        try {
            WorkflowVersion published = workflowCommands.createVersion(new CreateWorkflowVersionRequest(
                    publication.workflowId(), workflow.revision(), specification,
                    invocation.requestContext()));
            completePublished(publication, published.version(), invocation.requestContext());
        } catch (RevisionConflictException concurrentChange) {
            WorkflowVersion current = workflows.findLatestVersion(publication.workflowId()).orElseThrow();
            if (publicationMarker(current).equals(publication.publicationId().value())) {
                completePublished(publication, current.version(), invocation.requestContext());
            } else {
                completeConflict(publication, "workflow-version-changed", invocation.requestContext());
            }
        }
    }

    private WorkflowVersionSpec replaceTask(
            WorkflowVersion base,
            ScriptPublication publication,
            ScriptVersion script
    ) {
        List<WorkflowTaskDefinition> tasks = new ArrayList<>();
        boolean replaced = false;
        for (WorkflowTaskDefinition task : base.specification().tasks()) {
            if (!task.taskKey().equals(publication.taskKey())) {
                tasks.add(task);
                continue;
            }
            Map<String, String> attributes = new HashMap<>(task.attributes());
            attributes.put(PUBLICATION_ATTRIBUTE, publication.publicationId().value());
            attributes.put(SCRIPT_ID_ATTRIBUTE, publication.scriptId().value());
            attributes.put(SCRIPT_VERSION_ATTRIBUTE, Long.toString(publication.scriptVersion()));
            tasks.add(new WorkflowTaskDefinition(
                    task.taskKey(), task.displayName(), script.executionSpecification(),
                    task.retryPolicy(), task.timeout(), attributes));
            replaced = true;
        }
        if (!replaced) {
            throw new IllegalStateException("target task no longer exists in base workflow version");
        }
        Map<String, String> attributes = new HashMap<>(base.specification().attributes());
        attributes.put(PUBLICATION_ATTRIBUTE, publication.publicationId().value());
        return new WorkflowVersionSpec(
                tasks, base.specification().dependencies(), base.specification().schedules(),
                base.specification().runtimeBinding(), attributes);
    }

    private void completePublished(
            ScriptPublication publication,
            long workflowVersion,
            RequestContext requestContext
    ) {
        complete(publication, ScriptPublicationState.PUBLISHED,
                Optional.of(workflowVersion), Optional.empty(), requestContext);
    }

    private void completeConflict(
            ScriptPublication publication,
            String conflictCode,
            RequestContext requestContext
    ) {
        complete(publication, ScriptPublicationState.CONFLICTED,
                Optional.empty(), Optional.of(conflictCode), requestContext);
    }

    private ScriptPublication complete(
            ScriptPublication publication,
            ScriptPublicationState state,
            Optional<Long> publishedWorkflowVersion,
            Optional<String> conflictCode,
            RequestContext requestContext
    ) {
        Instant updatedAt = clock.now();
        ScriptPublicationTransitionResult transition = store.complete(
                publication, state, publishedWorkflowVersion, conflictCode, updatedAt);
        if (transition.changed()) {
            publishStateChange(
                    Optional.of(publication.state()), transition.publication(),
                    requestContext, updatedAt);
        }
        return transition.publication();
    }

    private ScriptVersion requireScriptVersion(RequestScriptPublication request) {
        return scripts.findVersion(request.scriptId(), request.scriptVersion())
                .orElseThrow(() -> new IllegalArgumentException("script version does not exist"));
    }

    private WorkflowDefinition requireWorkflowAtBase(RequestScriptPublication request) {
        WorkflowDefinition workflow = workflows.findWorkflow(request.workflowId())
                .orElseThrow(() -> new IllegalArgumentException("workflow does not exist"));
        if (workflow.latestVersion() != request.baseWorkflowVersion()) {
            throw new IllegalStateException("workflow latest version does not match baseWorkflowVersion");
        }
        return workflow;
    }

    private void requireTargetTask(RequestScriptPublication request) {
        WorkflowVersion base = workflows.findVersion(request.workflowId(), request.baseWorkflowVersion())
                .orElseThrow(() -> new IllegalArgumentException("base workflow version does not exist"));
        if (base.specification().tasks().stream()
                .noneMatch(task -> task.taskKey().equals(request.taskKey()))) {
            throw new IllegalArgumentException("target workflow task does not exist");
        }
    }

    private StoredScriptPublication requireStored(ScriptPublicationId publicationId) {
        return store.find(publicationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "publication does not exist: " + publicationId));
    }

    private static void requireSameRequest(
            StoredScriptPublication stored,
            RequestScriptPublication request
    ) {
        ScriptPublication publication = stored.publication();
        if (!publication.scriptId().equals(request.scriptId())
                || publication.scriptVersion() != request.scriptVersion()
                || !publication.workflowId().equals(request.workflowId())
                || publication.baseWorkflowVersion() != request.baseWorkflowVersion()
                || !publication.taskKey().equals(request.taskKey())
                || !stored.approvalTemplateKey().equals(request.approvalTemplateKey())
                || !stored.requestedBy().equals(request.requestedBy())
                || !stored.approvalAttributes().equals(request.attributes())) {
            throw new IllegalStateException("publication idempotency key was used for a different request");
        }
    }

    private static String publicationMarker(WorkflowVersion version) {
        return version.specification().attributes().getOrDefault(PUBLICATION_ATTRIBUTE, "");
    }

    private void publishStateChange(
            Optional<ScriptPublicationState> previousState,
            ScriptPublication publication,
            RequestContext requestContext,
            Instant occurredAt
    ) {
        eventPublisher.publish(new ScriptPublicationStateChangedEvent(
                idGenerator.nextIdValue(IdGenerationRequest.of("development", "domain-event")),
                occurredAt, requestContext, previousState, publication));
    }
}
