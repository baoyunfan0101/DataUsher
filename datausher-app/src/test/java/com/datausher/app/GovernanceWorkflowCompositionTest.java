package com.datausher.app;

import com.datausher.governance.access.api.Subject;
import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.access.api.SubjectStatus;
import com.datausher.governance.access.api.SubjectType;
import com.datausher.governance.access.core.DefaultIdentityQueryService;
import com.datausher.governance.access.core.InMemorySubjectStore;
import com.datausher.governance.approval.api.ApprovalCallbackDeliveryStatus;
import com.datausher.governance.approval.api.ApprovalCallbackHandler;
import com.datausher.governance.approval.api.ApprovalCallbackInvocation;
import com.datausher.governance.approval.api.ApprovalCallbackRef;
import com.datausher.governance.approval.api.ApprovalCallbackType;
import com.datausher.governance.approval.api.ApprovalDecisionType;
import com.datausher.governance.approval.api.ApprovalPurpose;
import com.datausher.governance.approval.api.ApprovalRequestStatus;
import com.datausher.governance.approval.api.ApprovalStepDefinition;
import com.datausher.governance.approval.api.ApprovalTemplateKey;
import com.datausher.governance.approval.api.ApproverSelector;
import com.datausher.governance.approval.api.DecideApprovalRequest;
import com.datausher.governance.approval.api.PublishApprovalTemplateRequest;
import com.datausher.governance.approval.api.SubmitApprovalRequest;
import com.datausher.governance.approval.core.DefaultApprovalCallbackRegistry;
import com.datausher.governance.approval.core.DefaultApprovalService;
import com.datausher.governance.approval.core.AuthenticatedSubjectDecisionAuthorizer;
import com.datausher.governance.approval.core.DirectSubjectApproverResolver;
import com.datausher.governance.approval.core.InMemoryApprovalCallbackStore;
import com.datausher.governance.approval.core.InMemoryApprovalStore;
import com.datausher.governance.approval.core.ResourceOwnerApproverResolver;
import com.datausher.governance.ownership.api.AssignResourceOwnerRequest;
import com.datausher.governance.ownership.api.OwnershipRole;
import com.datausher.governance.ownership.core.DefaultOwnershipService;
import com.datausher.governance.ownership.core.InMemoryOwnershipStore;
import com.datausher.governance.resource.api.RegisterResourceRequest;
import com.datausher.governance.resource.api.RegisterResourceTypeRequest;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.governance.resource.api.ResourceTypeDefinition;
import com.datausher.governance.resource.core.DefaultResourceRegistryService;
import com.datausher.governance.resource.core.InMemoryResourceStore;
import com.datausher.platform.audit.core.CompensatingAuditedCommandExecutor;
import com.datausher.platform.audit.core.DefaultAuditService;
import com.datausher.platform.audit.core.InMemoryAuditEventStore;
import com.datausher.platform.notification.api.NotificationChannel;
import com.datausher.platform.notification.api.NotificationChannelProvider;
import com.datausher.platform.notification.api.NotificationChannelResult;
import com.datausher.platform.notification.api.NotificationContent;
import com.datausher.platform.notification.api.NotificationDispatchStatus;
import com.datausher.platform.notification.api.NotificationEnvelope;
import com.datausher.platform.notification.api.NotificationRecipient;
import com.datausher.platform.notification.api.NotificationRecipientType;
import com.datausher.platform.notification.api.NotificationRoute;
import com.datausher.platform.notification.api.NotificationTemplateKey;
import com.datausher.platform.notification.api.PublishNotificationTemplateRequest;
import com.datausher.platform.notification.api.SendNotificationRequest;
import com.datausher.platform.notification.core.DefaultNotificationService;
import com.datausher.platform.notification.core.InMemoryNotificationStore;
import com.datausher.platform.notification.core.StrictNotificationTemplateRenderer;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.context.ActorContext;
import com.datausher.platform.shared.event.core.NoopDomainEventPublisher;
import com.datausher.platform.shared.id.core.UuidIdGenerator;
import com.datausher.platform.shared.time.core.SystemClock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GovernanceWorkflowCompositionTest {
    @Test
    void composesOwnershipApprovalCallbacksAndChannelNeutralNotifications() {
        var ids = new UuidIdGenerator();
        var clock = new SystemClock();
        var audit = new DefaultAuditService(new InMemoryAuditEventStore(), ids, clock);
        var auditedCommands = new CompensatingAuditedCommandExecutor(audit);
        var events = new NoopDomainEventPublisher();
        var resources = new DefaultResourceRegistryService(
                new InMemoryResourceStore(), clock, auditedCommands, scope -> {
                });
        var subjectStore = new InMemorySubjectStore();
        var identities = new DefaultIdentityQueryService(subjectStore);
        var ownership = new DefaultOwnershipService(
                new InMemoryOwnershipStore(), resources, identities, ids, clock, auditedCommands, events);
        var delivered = new AtomicReference<NotificationEnvelope>();
        NotificationChannel channel = new NotificationChannel("recording");
        NotificationChannelProvider provider = new NotificationChannelProvider() {
            @Override
            public NotificationChannel channel() {
                return channel;
            }

            @Override
            public NotificationChannelResult deliver(NotificationEnvelope envelope) {
                delivered.set(envelope);
                return new NotificationChannelResult("message-1", Map.of());
            }
        };
        var notifications = new DefaultNotificationService(
                new InMemoryNotificationStore(), new StrictNotificationTemplateRenderer(), List.of(provider),
                ids, clock, auditedCommands, audit);
        var callbacks = new DefaultApprovalCallbackRegistry(
                new InMemoryApprovalCallbackStore(), clock, audit);
        var approvals = new DefaultApprovalService(
                new InMemoryApprovalStore(), resources, identities,
                new AuthenticatedSubjectDecisionAuthorizer(),
                List.of(new DirectSubjectApproverResolver(), new ResourceOwnerApproverResolver(ownership)),
                ids, clock, auditedCommands, events, callbacks);
        RequestContext context = RequestContext.system("request-1", Instant.now());
        SubjectRef owner = new SubjectRef(SubjectType.USER, "owner-1");
        subjectStore.save(new Subject(owner, "Owner", SubjectStatus.ACTIVE, Map.of()));
        resources.register(new RegisterResourceTypeRequest(
                new ResourceTypeDefinition(
                        "workflow", "workflow-core", "Workflow", Set.of("read", "publish")), context));
        ResourceRef workflow = ResourceRef.global("workflow", "daily-orders");
        resources.register(new RegisterResourceRequest(workflow, "Daily Orders", Map.of(), context));
        ownership.assign(new AssignResourceOwnerRequest(
                workflow, owner, OwnershipRole.PRIMARY, Map.of(), context));
        NotificationTemplateKey notificationTemplate = new NotificationTemplateKey("approval-completed");
        notifications.publishTemplate(new PublishNotificationTemplateRequest(
                notificationTemplate, "Approval Completed",
                List.of(new NotificationRoute(channel,
                        new NotificationContent("text/plain", "Approved", "Approved ${resource}", Map.of()))),
                Map.of(), context));
        ApprovalCallbackType callbackType = new ApprovalCallbackType("workflow-publish");
        callbacks.register(new ApprovalCallbackHandler() {
            @Override
            public ApprovalCallbackType callbackType() {
                return callbackType;
            }

            @Override
            public void handle(ApprovalCallbackInvocation invocation) {
                var dispatch = notifications.send(new SendNotificationRequest(
                        notificationTemplate,
                        List.of(new NotificationRecipient(
                                NotificationRecipientType.SUBJECT, owner.canonicalValue(), Map.of())),
                        Map.of("resource", invocation.targetResource().resourceId()),
                        invocation.approvalRequestId().value(), Map.of(), invocation.requestContext()));
                assertEquals(NotificationDispatchStatus.SUCCEEDED, dispatch.status());
            }
        });
        ApprovalTemplateKey approvalTemplate = new ApprovalTemplateKey("workflow-publish");
        approvals.publishTemplate(new PublishApprovalTemplateRequest(
                approvalTemplate, "Workflow Publish", new ApprovalPurpose("workflow-publish"),
                List.of(new ApprovalStepDefinition(
                        "owner-review", "Owner Review",
                        List.of(ApproverSelector.resourceOwner(OwnershipRole.PRIMARY)), 1)),
                Map.of(), context));

        var request = approvals.submit(new SubmitApprovalRequest(
                approvalTemplate, "Publish Daily Orders", workflow, owner,
                new ApprovalCallbackRef(callbackType, workflow.resourceId(), Map.of()),
                "publish-daily-orders", Map.of(), context));
        request = approvals.decide(new DecideApprovalRequest(
                request.requestId(), "owner-review", owner,
                ApprovalDecisionType.APPROVE, "approved",
                new RequestContext(
                        "decision-1",
                        new ActorContext(
                                owner.subjectId(), owner.subjectId(),
                                Set.of(owner.canonicalValue()), Map.of()),
                        Instant.now(), Map.of())));

        assertEquals(ApprovalRequestStatus.APPROVED, request.status());
        assertEquals(ApprovalCallbackDeliveryStatus.SUCCEEDED,
                callbacks.findDelivery(request.requestId()).orElseThrow().status());
        assertEquals("Approved daily-orders", delivered.get().content().body());
    }
}
