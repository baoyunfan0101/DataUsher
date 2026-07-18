package com.datausher.development.core;

import com.datausher.development.api.RequestScriptPublication;
import com.datausher.development.api.ScriptDefinition;
import com.datausher.development.api.ScriptId;
import com.datausher.development.api.ScriptPublicationState;
import com.datausher.development.api.ScriptQueryService;
import com.datausher.development.api.ScriptVersion;
import com.datausher.execution.api.ExecutionAccountId;
import com.datausher.execution.api.ExecutionQueueId;
import com.datausher.execution.api.ExecutionResultMode;
import com.datausher.execution.api.ExecutionSpecification;
import com.datausher.execution.api.ExecutionWorkload;
import com.datausher.execution.api.ExecutionWorkloadType;
import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.access.api.SubjectType;
import com.datausher.governance.approval.api.ApprovalCallbackInvocation;
import com.datausher.governance.approval.api.ApprovalCommandService;
import com.datausher.governance.approval.api.ApprovalPurpose;
import com.datausher.governance.approval.api.ApprovalRequest;
import com.datausher.governance.approval.api.ApprovalRequestId;
import com.datausher.governance.approval.api.ApprovalRequestStatus;
import com.datausher.governance.approval.api.ApprovalStep;
import com.datausher.governance.approval.api.ApprovalStepStatus;
import com.datausher.governance.approval.api.ApprovalTemplate;
import com.datausher.governance.approval.api.ApprovalTemplateKey;
import com.datausher.governance.approval.api.CancelApprovalRequest;
import com.datausher.governance.approval.api.ChangeApprovalTemplateStatusRequest;
import com.datausher.governance.approval.api.DecideApprovalRequest;
import com.datausher.governance.approval.api.PublishApprovalTemplateRequest;
import com.datausher.governance.approval.api.SubmitApprovalRequest;
import com.datausher.governance.resource.api.RegisteredResource;
import com.datausher.governance.resource.api.ResourceLifecycle;
import com.datausher.governance.resource.api.ResourceQuery;
import com.datausher.governance.resource.api.ResourceQueryService;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.platform.audit.core.CompensatingAuditedCommandExecutor;
import com.datausher.platform.audit.core.DefaultAuditService;
import com.datausher.platform.audit.core.InMemoryAuditEventStore;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.id.core.UuidIdGenerator;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;
import com.datausher.platform.shared.time.core.SystemClock;
import com.datausher.workflow.api.CreateWorkflowRequest;
import com.datausher.workflow.api.CreateWorkflowVersionRequest;
import com.datausher.workflow.api.TaskRetryPolicy;
import com.datausher.workflow.api.WorkflowId;
import com.datausher.workflow.api.WorkflowTaskDefinition;
import com.datausher.workflow.api.WorkflowVersion;
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

class DefaultScriptPublicationServiceTest {
    @Test
    void createsANewWorkflowVersionOnlyAfterApproval() {
        Fixture fixture = fixture();
        var publication = fixture.publications.request(fixture.request("publish-key"));

        assertEquals(ScriptPublicationState.APPROVAL_PENDING, publication.state());
        assertEquals(1, fixture.workflows.findWorkflow(fixture.workflowId).orElseThrow().latestVersion());

        fixture.publications.handle(approved(fixture.approvals.lastRequest, publication, fixture.context));
        fixture.publications.handle(approved(fixture.approvals.lastRequest, publication, fixture.context));

        var completed = fixture.publications.findPublication(publication.publicationId()).orElseThrow();
        WorkflowVersion workflowVersion = fixture.workflows.findLatestVersion(fixture.workflowId).orElseThrow();
        assertEquals(ScriptPublicationState.PUBLISHED, completed.state());
        assertEquals(2, workflowVersion.version());
        assertEquals("new-payload",
                workflowVersion.specification().tasks().getFirst()
                        .executionSpecification().workload().payload());
    }

    @Test
    void recordsAConflictInsteadOfOverwritingConcurrentWorkflowChanges() {
        Fixture fixture = fixture();
        var publication = fixture.publications.request(fixture.request("publish-key"));
        var workflow = fixture.workflows.findWorkflow(fixture.workflowId).orElseThrow();
        fixture.workflows.createVersion(new CreateWorkflowVersionRequest(
                fixture.workflowId, workflow.revision(),
                new WorkflowVersionSpec(
                        List.of(task("old-payload")), List.of(), Optional.empty(),
                        Map.of("concurrent", "change")), fixture.context));

        fixture.publications.handle(approved(fixture.approvals.lastRequest, publication, fixture.context));

        var completed = fixture.publications.findPublication(publication.publicationId()).orElseThrow();
        assertEquals(ScriptPublicationState.CONFLICTED, completed.state());
        assertEquals("workflow-version-changed", completed.conflictCode().orElseThrow());
        assertEquals(2, fixture.workflows.findWorkflow(fixture.workflowId).orElseThrow().latestVersion());
    }

    private static Fixture fixture() {
        ResourceRef workflowRef = ResourceRef.global("workflow", "workflow-1");
        var clock = new SystemClock();
        var ids = new UuidIdGenerator();
        var audit = new DefaultAuditService(new InMemoryAuditEventStore(), ids, clock);
        var workflows = new DefaultWorkflowService(
                new InMemoryWorkflowStore(), resources(workflowRef), clock,
                new CompensatingAuditedCommandExecutor(audit));
        RequestContext context = RequestContext.system("request-1", Instant.now());
        WorkflowId workflowId = new WorkflowId("workflow-1");
        var workflow = workflows.create(new CreateWorkflowRequest(
                workflowId, workflowRef, "Workflow", Map.of(), context));
        workflows.createVersion(new CreateWorkflowVersionRequest(
                workflowId, workflow.revision(), new WorkflowVersionSpec(
                        List.of(task("old-payload")), List.of(), Optional.empty(), Map.of()), context));
        ScriptVersion script = new ScriptVersion(
                new ScriptId("script-1"), 1, specification("new-payload"),
                Instant.EPOCH, "actor-1", Map.of());
        var approvals = new RecordingApprovals();
        var publications = new DefaultScriptPublicationService(
                scripts(script), workflows, workflows, approvals,
                new InMemoryScriptPublicationStore(), ids, clock);
        return new Fixture(workflows, publications, approvals, workflowId, context);
    }

    private static ApprovalCallbackInvocation approved(
            SubmitApprovalRequest approval,
            com.datausher.development.api.ScriptPublication publication,
            RequestContext context
    ) {
        return new ApprovalCallbackInvocation(
                new ApprovalRequestId("approval-1"), ApprovalRequestStatus.APPROVED,
                approval.targetResource(), DefaultScriptPublicationService.CALLBACK_TYPE,
                publication.publicationId().value(), Map.of(), context);
    }

    private static WorkflowTaskDefinition task(String payload) {
        return new WorkflowTaskDefinition(
                "task-1", "Task", specification(payload), TaskRetryPolicy.NONE,
                Duration.ofMinutes(10), Map.of());
    }

    private static ExecutionSpecification specification(String payload) {
        return new ExecutionSpecification(
                new ExecutionQueueId("default"), new ExecutionAccountId("local"),
                new ExecutionWorkload(
                        new ExecutionWorkloadType("vendor-task"), payload, Map.of(), Map.of()),
                ExecutionResultMode.REFERENCE, 100);
    }

    private static ScriptQueryService scripts(ScriptVersion version) {
        return new ScriptQueryService() {
            @Override
            public Optional<ScriptDefinition> findScript(ScriptId scriptId) {
                return Optional.empty();
            }

            @Override
            public Optional<ScriptVersion> findVersion(ScriptId scriptId, long candidateVersion) {
                return version.scriptId().equals(scriptId) && version.version() == candidateVersion
                        ? Optional.of(version) : Optional.empty();
            }

            @Override
            public Optional<ScriptVersion> findLatestVersion(ScriptId scriptId) {
                return version.scriptId().equals(scriptId) ? Optional.of(version) : Optional.empty();
            }

            @Override
            public List<ScriptVersion> listVersions(ScriptId scriptId) {
                return version.scriptId().equals(scriptId) ? List.of(version) : List.of();
            }
        };
    }

    private static ResourceQueryService resources(ResourceRef ref) {
        RegisteredResource resource = new RegisteredResource(
                ref, "Workflow", ResourceLifecycle.ACTIVE, Instant.now(), "system", Map.of());
        return new ResourceQueryService() {
            @Override
            public Optional<RegisteredResource> find(ResourceRef candidate) {
                return ref.equals(candidate) ? Optional.of(resource) : Optional.empty();
            }

            @Override
            public PageResult<RegisteredResource> search(ResourceQuery query, PageRequest pageRequest) {
                return PageResult.empty(pageRequest);
            }
        };
    }

    private record Fixture(
            DefaultWorkflowService workflows,
            DefaultScriptPublicationService publications,
            RecordingApprovals approvals,
            WorkflowId workflowId,
            RequestContext context
    ) {
        RequestScriptPublication request(String idempotencyKey) {
            return new RequestScriptPublication(
                    new ScriptId("script-1"), 1, workflowId, 1, "task-1",
                    new ApprovalTemplateKey("script-publish"),
                    new SubjectRef(SubjectType.USER, "user-1"), idempotencyKey,
                    Map.of(), context);
        }
    }

    private static final class RecordingApprovals implements ApprovalCommandService {
        private SubmitApprovalRequest lastRequest;

        @Override
        public ApprovalRequest submit(SubmitApprovalRequest request) {
            lastRequest = request;
            SubjectRef approver = new SubjectRef(SubjectType.USER, "approver-1");
            return new ApprovalRequest(
                    new ApprovalRequestId("approval-1"), request.templateKey(), 1,
                    new ApprovalPurpose("publication"), request.title(), request.targetResource(),
                    request.requestedBy(), ApprovalRequestStatus.PENDING,
                    List.of(new ApprovalStep(
                            "review", "Review", Set.of(approver), 1, Set.of(),
                            ApprovalStepStatus.ACTIVE, List.of())),
                    request.callback(), request.idempotencyKey(), Instant.now(), null,
                    request.attributes());
        }

        @Override
        public ApprovalTemplate publishTemplate(PublishApprovalTemplateRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ApprovalTemplate changeTemplateStatus(ChangeApprovalTemplateStatusRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ApprovalRequest decide(DecideApprovalRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ApprovalRequest cancel(CancelApprovalRequest request) {
            throw new UnsupportedOperationException();
        }
    }
}
