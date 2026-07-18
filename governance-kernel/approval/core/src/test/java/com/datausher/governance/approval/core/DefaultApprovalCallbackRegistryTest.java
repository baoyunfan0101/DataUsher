package com.datausher.governance.approval.core;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.access.api.SubjectType;
import com.datausher.governance.approval.api.ApprovalCallbackDeliveryStatus;
import com.datausher.governance.approval.api.ApprovalCallbackHandler;
import com.datausher.governance.approval.api.ApprovalCallbackRef;
import com.datausher.governance.approval.api.ApprovalCallbackType;
import com.datausher.governance.approval.api.ApprovalPurpose;
import com.datausher.governance.approval.api.ApprovalRequest;
import com.datausher.governance.approval.api.ApprovalRequestId;
import com.datausher.governance.approval.api.ApprovalRequestStatus;
import com.datausher.governance.approval.api.ApprovalStep;
import com.datausher.governance.approval.api.ApprovalStepStatus;
import com.datausher.governance.approval.api.ApprovalTemplateKey;
import com.datausher.governance.approval.api.RetryApprovalCallbackRequest;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.platform.audit.core.DefaultAuditService;
import com.datausher.platform.audit.core.InMemoryAuditEventStore;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.id.core.UuidIdGenerator;
import com.datausher.platform.shared.time.core.SystemClock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultApprovalCallbackRegistryTest {
    @Test
    void recordsFailureAndRetriesCallbackIdempotently() {
        var clock = new SystemClock();
        var audit = new DefaultAuditService(new InMemoryAuditEventStore(), new UuidIdGenerator(), clock);
        var registry = new DefaultApprovalCallbackRegistry(
                new InMemoryApprovalCallbackStore(), clock, audit);
        var attempts = new AtomicInteger();
        ApprovalCallbackType type = new ApprovalCallbackType("workflow-publish");
        registry.register(new ApprovalCallbackHandler() {
            @Override
            public ApprovalCallbackType callbackType() {
                return type;
            }

            @Override
            public void handle(com.datausher.governance.approval.api.ApprovalCallbackInvocation invocation) {
                if (attempts.incrementAndGet() == 1) {
                    throw new IllegalStateException("temporary failure");
                }
            }
        });
        RequestContext context = RequestContext.system("request-1", Instant.now());
        ApprovalRequest approval = terminalApproval(type);

        registry.handle(approval, context);
        assertEquals(ApprovalCallbackDeliveryStatus.FAILED,
                registry.findDelivery(approval.requestId()).orElseThrow().status());

        var delivery = registry.retry(new RetryApprovalCallbackRequest(approval.requestId(), context));
        registry.handle(approval, context);

        assertEquals(ApprovalCallbackDeliveryStatus.SUCCEEDED, delivery.status());
        assertEquals(2, attempts.get());
    }

    private static ApprovalRequest terminalApproval(ApprovalCallbackType type) {
        SubjectRef subject = new SubjectRef(SubjectType.USER, "approver");
        ApprovalStep step = new ApprovalStep(
                "review", "Review", Set.of(subject), 1, ApprovalStepStatus.APPROVED, List.of());
        return new ApprovalRequest(
                new ApprovalRequestId("approval-1"), new ApprovalTemplateKey("workflow-publish"), 1,
                new ApprovalPurpose("workflow-publish"), "Publish", ResourceRef.global("workflow", "daily"),
                subject, ApprovalRequestStatus.APPROVED, List.of(step),
                new ApprovalCallbackRef(type, "daily", Map.of()),
                Instant.parse("2026-07-18T00:00:00Z"), Instant.parse("2026-07-18T00:01:00Z"), Map.of());
    }
}
