package com.datausher.app;

import com.datausher.development.api.CreateScriptRequest;
import com.datausher.development.api.CreateScriptVersionRequest;
import com.datausher.development.api.RequestScriptPublication;
import com.datausher.development.api.ScriptId;
import com.datausher.development.api.ScriptLanguage;
import com.datausher.development.api.ScriptPublicationState;
import com.datausher.development.api.StartDebugRunRequest;
import com.datausher.development.core.DefaultDebugRunService;
import com.datausher.development.core.DefaultScriptPublicationService;
import com.datausher.development.core.DefaultScriptService;
import com.datausher.development.core.InMemoryDebugRunStore;
import com.datausher.development.core.InMemoryScriptPublicationStore;
import com.datausher.development.core.InMemoryScriptStore;
import com.datausher.execution.api.CancelExecutionRequest;
import com.datausher.execution.api.ExecutionAccountId;
import com.datausher.execution.api.ExecutionCommandService;
import com.datausher.execution.api.ExecutionQueueId;
import com.datausher.execution.api.ExecutionRequest;
import com.datausher.execution.api.ExecutionRequestId;
import com.datausher.execution.api.ExecutionResultMode;
import com.datausher.execution.api.ExecutionSpecification;
import com.datausher.execution.api.ExecutionState;
import com.datausher.execution.api.ExecutionWorkload;
import com.datausher.execution.api.ExecutionWorkloadType;
import com.datausher.execution.api.SubmitExecutionRequest;
import com.datausher.governance.access.api.Subject;
import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.access.api.SubjectStatus;
import com.datausher.governance.access.api.SubjectType;
import com.datausher.governance.access.core.DefaultIdentityQueryService;
import com.datausher.governance.access.core.InMemorySubjectStore;
import com.datausher.governance.approval.api.ApprovalDecisionType;
import com.datausher.governance.approval.api.ApprovalPurpose;
import com.datausher.governance.approval.api.ApprovalStepDefinition;
import com.datausher.governance.approval.api.ApprovalTemplateKey;
import com.datausher.governance.approval.api.ApproverSelector;
import com.datausher.governance.approval.api.DecideApprovalRequest;
import com.datausher.governance.approval.api.PublishApprovalTemplateRequest;
import com.datausher.governance.approval.core.AuthenticatedSubjectDecisionAuthorizer;
import com.datausher.governance.approval.core.DefaultApprovalCallbackRegistry;
import com.datausher.governance.approval.core.DefaultApprovalService;
import com.datausher.governance.approval.core.DirectSubjectApproverResolver;
import com.datausher.governance.approval.core.InMemoryApprovalCallbackStore;
import com.datausher.governance.approval.core.InMemoryApprovalStore;
import com.datausher.governance.resource.api.RegisterResourceRequest;
import com.datausher.governance.resource.api.RegisterResourceTypeRequest;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.governance.resource.api.ResourceTypeDefinition;
import com.datausher.governance.resource.core.DefaultResourceRegistryService;
import com.datausher.governance.resource.core.InMemoryResourceStore;
import com.datausher.platform.audit.core.CompensatingAuditedCommandExecutor;
import com.datausher.platform.audit.core.DefaultAuditService;
import com.datausher.platform.audit.core.InMemoryAuditEventStore;
import com.datausher.platform.shared.context.ActorContext;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.core.NoopDomainEventPublisher;
import com.datausher.platform.shared.id.core.UuidIdGenerator;
import com.datausher.platform.shared.time.core.SystemClock;
import com.datausher.workflow.api.CreateWorkflowRequest;
import com.datausher.workflow.api.CreateWorkflowVersionRequest;
import com.datausher.workflow.api.TaskRetryPolicy;
import com.datausher.workflow.api.WorkflowId;
import com.datausher.workflow.api.WorkflowTaskDefinition;
import com.datausher.workflow.api.WorkflowVersionSpec;
import com.datausher.workflow.core.DefaultWorkflowService;
import com.datausher.workflow.core.InMemoryWorkflowStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkflowDevelopmentCompositionTest {
    @Test
    void debugsAndPublishesVersionedScriptsThroughStableModuleBoundaries() {
        var ids = new UuidIdGenerator();
        var clock = new SystemClock();
        var events = new NoopDomainEventPublisher();
        var audit = new DefaultAuditService(new InMemoryAuditEventStore(), ids, clock);
        var auditedCommands = new CompensatingAuditedCommandExecutor(audit);
        var resources = new DefaultResourceRegistryService(
                new InMemoryResourceStore(), clock, auditedCommands, scope -> { });
        var subjects = new InMemorySubjectStore();
        var identities = new DefaultIdentityQueryService(subjects);
        var callbacks = new DefaultApprovalCallbackRegistry(
                new InMemoryApprovalCallbackStore(), clock, audit);
        var approvals = new DefaultApprovalService(
                new InMemoryApprovalStore(), resources, identities,
                new AuthenticatedSubjectDecisionAuthorizer(),
                List.of(new DirectSubjectApproverResolver()), ids, clock,
                auditedCommands, events, callbacks);
        RequestContext context = RequestContext.system("request-1", clock.now());
        SubjectRef developer = new SubjectRef(SubjectType.USER, "developer-1");
        subjects.save(new Subject(developer, "Developer", SubjectStatus.ACTIVE, Map.of()));

        registerResourceTypes(resources, context);
        ResourceRef workflowRef = ResourceRef.global("workflow", "daily-orders");
        ResourceRef scriptRef = ResourceRef.global("script", "extract-orders");
        resources.register(new RegisterResourceRequest(
                workflowRef, "Daily Orders", Map.of(), context));
        resources.register(new RegisterResourceRequest(
                scriptRef, "Extract Orders", Map.of(), context));

        var workflows = new DefaultWorkflowService(
                new InMemoryWorkflowStore(), resources, clock, auditedCommands,
                ids, events);
        WorkflowId workflowId = new WorkflowId("daily-orders");
        var workflow = workflows.create(new CreateWorkflowRequest(
                workflowId, workflowRef, "Daily Orders", Map.of(), context));
        workflows.createVersion(new CreateWorkflowVersionRequest(
                workflowId, workflow.revision(), new WorkflowVersionSpec(
                        List.of(task(specification("old-payload"))), List.of(),
                        Optional.empty(), Map.of()), context));

        var scripts = new DefaultScriptService(
                new InMemoryScriptStore(), resources, clock, auditedCommands,
                ids, events);
        ScriptId scriptId = new ScriptId("extract-orders");
        var script = scripts.create(new CreateScriptRequest(
                scriptId, scriptRef, "Extract Orders", new ScriptLanguage("vendor.lang"),
                Map.of(), context));
        scripts.createVersion(new CreateScriptVersionRequest(
                scriptId, script.revision(), specification("new-payload"), Map.of(), context));

        var execution = new RecordingExecutionService();
        var debugRuns = new DefaultDebugRunService(
                scripts, new InMemoryDebugRunStore(), execution, ids, clock, events);
        var debug = debugRuns.start(new StartDebugRunRequest(
                scriptId, 1, "debug-1", Map.of(), context));
        debugRuns.dispatch(debug.debugRunId(), context);

        ApprovalTemplateKey templateKey = new ApprovalTemplateKey("script-publication");
        approvals.publishTemplate(new PublishApprovalTemplateRequest(
                templateKey, "Script Publication", new ApprovalPurpose("script-publication"),
                List.of(new ApprovalStepDefinition(
                        "review", "Review", List.of(ApproverSelector.subject(developer)), 1)),
                Map.of(), context));
        var publications = new DefaultScriptPublicationService(
                scripts, workflows, workflows, approvals,
                new InMemoryScriptPublicationStore(), ids, clock, events);
        callbacks.register(publications);
        var publication = publications.request(new RequestScriptPublication(
                scriptId, 1, workflowId, 1, "extract", templateKey, developer,
                "publication-1", Map.of(), context));
        RequestContext decisionContext = new RequestContext(
                "decision-1",
                new ActorContext(
                        developer.subjectId(), developer.subjectId(),
                        Set.of(developer.canonicalValue()), Map.of()),
                clock.now(), Map.of());
        approvals.decide(new DecideApprovalRequest(
                publication.approvalRequestId().orElseThrow(), "review", developer,
                ApprovalDecisionType.APPROVE, "approved", decisionContext));

        var completed = publications.findPublication(publication.publicationId()).orElseThrow();
        var publishedWorkflow = workflows.findLatestVersion(workflowId).orElseThrow();
        assertEquals("debug-run", execution.submission.origin().type().value());
        assertEquals(ScriptPublicationState.PUBLISHED, completed.state());
        assertEquals(2, publishedWorkflow.version());
        assertEquals("new-payload",
                publishedWorkflow.specification().tasks().getFirst()
                        .executionSpecification().workload().payload());
    }

    private static void registerResourceTypes(
            DefaultResourceRegistryService resources,
            RequestContext context
    ) {
        resources.register(new RegisterResourceTypeRequest(
                new ResourceTypeDefinition(
                        "workflow", "workflow-core", "Workflow", Set.of("read", "publish")),
                context));
        resources.register(new RegisterResourceTypeRequest(
                new ResourceTypeDefinition(
                        "script", "development-lifecycle", "Script", Set.of("read", "publish")),
                context));
    }

    private static WorkflowTaskDefinition task(ExecutionSpecification specification) {
        return new WorkflowTaskDefinition(
                "extract", "Extract", specification, TaskRetryPolicy.NONE,
                Duration.ofMinutes(10), Map.of());
    }

    private static ExecutionSpecification specification(String payload) {
        return new ExecutionSpecification(
                new ExecutionQueueId("default"), new ExecutionAccountId("local"),
                new ExecutionWorkload(
                        new ExecutionWorkloadType("vendor-task"), payload, Map.of(), Map.of()),
                ExecutionResultMode.REFERENCE, 100);
    }

    private static final class RecordingExecutionService implements ExecutionCommandService {
        private SubmitExecutionRequest submission;

        @Override
        public ExecutionRequest submit(SubmitExecutionRequest request) {
            submission = request;
            Instant now = Instant.now();
            return new ExecutionRequest(
                    new ExecutionRequestId("execution-1"), request.specification(),
                    request.idempotencyKey(), request.origin(), ExecutionState.PENDING,
                    now, now, Optional.empty(), 1);
        }

        @Override
        public ExecutionRequest cancel(CancelExecutionRequest request) {
            throw new UnsupportedOperationException();
        }
    }
}
