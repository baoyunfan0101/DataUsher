package com.datausher.governance.approval.core;

import com.datausher.governance.access.api.*;
import com.datausher.governance.approval.api.*;
import com.datausher.governance.resource.api.*;
import com.datausher.platform.audit.core.CompensatingAuditedCommandExecutor;
import com.datausher.platform.audit.core.DefaultAuditService;
import com.datausher.platform.audit.core.InMemoryAuditEventStore;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.id.core.UuidIdGenerator;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;
import com.datausher.platform.shared.time.core.SystemClock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultApprovalServiceTest {
    @Test
    void snapshotsTemplateAndAdvancesSequentialSteps() {
        ResourceRef target = ResourceRef.global("workflow", "daily-orders");
        SubjectRef requester = new SubjectRef(SubjectType.USER, "requester");
        SubjectRef reviewer = new SubjectRef(SubjectType.USER, "reviewer");
        SubjectRef operator = new SubjectRef(SubjectType.USER, "operator");
        DefaultApprovalService service = service(target, List.of(requester, reviewer, operator));
        RequestContext context = RequestContext.system("request-1", Instant.now());
        ApprovalTemplateKey key = new ApprovalTemplateKey("workflow-publish");

        service.publishTemplate(new PublishApprovalTemplateRequest(
                key, "Workflow Publish", new ApprovalPurpose("workflow-publish"),
                List.of(
                        new ApprovalStepDefinition("review", "Review",
                                List.of(ApproverSelector.subject(reviewer)), 1),
                        new ApprovalStepDefinition("operate", "Operate",
                                List.of(ApproverSelector.subject(operator)), 1)
                ), Map.of(), context));
        ApprovalRequest request = service.submit(new SubmitApprovalRequest(
                key, "Publish daily orders", target, requester, null,
                "publish-daily-orders", Map.of(), context));
        ApprovalRequest duplicate = service.submit(new SubmitApprovalRequest(
                key, "Publish daily orders", target, requester, null,
                "publish-daily-orders", Map.of(), context));

        assertEquals(request.requestId(), duplicate.requestId());
        assertThrows(IllegalStateException.class, () -> service.submit(new SubmitApprovalRequest(
                key, "Different request", target, requester, null,
                "publish-daily-orders", Map.of(), context)));

        request = service.decide(new DecideApprovalRequest(
                request.requestId(), "review", reviewer, ApprovalDecisionType.APPROVE, "ok", context));
        assertEquals(ApprovalRequestStatus.PENDING, request.status());
        assertEquals(ApprovalStepStatus.ACTIVE, request.steps().get(1).status());
        assertEquals(request, service.decide(new DecideApprovalRequest(
                request.requestId(), "review", reviewer, ApprovalDecisionType.APPROVE, "ok", context)));

        request = service.decide(new DecideApprovalRequest(
                request.requestId(), "operate", operator, ApprovalDecisionType.APPROVE, "ok", context));
        assertEquals(ApprovalRequestStatus.APPROVED, request.status());
        assertEquals(1, request.templateVersion());
    }

    @Test
    void rejectionSkipsRemainingSteps() {
        ResourceRef target = ResourceRef.global("workflow", "daily-orders");
        SubjectRef requester = new SubjectRef(SubjectType.USER, "requester");
        SubjectRef reviewer = new SubjectRef(SubjectType.USER, "reviewer");
        DefaultApprovalService service = service(target, List.of(requester, reviewer));
        RequestContext context = RequestContext.system("request-1", Instant.now());
        ApprovalTemplateKey key = new ApprovalTemplateKey("workflow-publish");
        service.publishTemplate(new PublishApprovalTemplateRequest(
                key, "Workflow Publish", new ApprovalPurpose("workflow-publish"),
                List.of(new ApprovalStepDefinition("review", "Review",
                        List.of(ApproverSelector.subject(reviewer)), 1)), Map.of(), context));
        ApprovalRequest request = service.submit(new SubmitApprovalRequest(
                key, "Publish daily orders", target, requester, null,
                "publish-daily-orders", Map.of(), context));

        request = service.decide(new DecideApprovalRequest(
                request.requestId(), "review", reviewer, ApprovalDecisionType.REJECT, "unsafe", context));

        assertEquals(ApprovalRequestStatus.REJECTED, request.status());
        assertEquals(ApprovalStepStatus.REJECTED, request.steps().get(0).status());
    }

    private static DefaultApprovalService service(ResourceRef target, List<SubjectRef> subjects) {
        var clock = new SystemClock();
        var ids = new UuidIdGenerator();
        var audit = new DefaultAuditService(new InMemoryAuditEventStore(), ids, clock);
        return new DefaultApprovalService(
                new InMemoryApprovalStore(), resources(target), identities(subjects),
                List.of(new DirectSubjectApproverResolver()), ids, clock,
                new CompensatingAuditedCommandExecutor(audit), ApprovalTerminalHandler.noop());
    }

    private static ResourceQueryService resources(ResourceRef ref) {
        RegisteredResource resource = new RegisteredResource(
                ref, "Daily Orders", ResourceLifecycle.ACTIVE, Instant.now(), "system", Map.of());
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

    private static IdentityQueryService identities(List<SubjectRef> refs) {
        return new IdentityQueryService() {
            @Override
            public Optional<Subject> find(SubjectRef candidate) {
                return refs.contains(candidate)
                        ? Optional.of(new Subject(candidate, candidate.subjectId(), SubjectStatus.ACTIVE, Map.of()))
                        : Optional.empty();
            }

            @Override
            public PageResult<Subject> search(SubjectQuery query, PageRequest pageRequest) {
                return PageResult.empty(pageRequest);
            }
        };
    }
}
