package com.datausher.governance.approval.core;

import com.datausher.governance.approval.api.ApprovalCallbackDelivery;
import com.datausher.governance.approval.api.ApprovalCallbackDeliveryService;
import com.datausher.governance.approval.api.ApprovalCallbackDeliveryStatus;
import com.datausher.governance.approval.api.ApprovalCallbackHandler;
import com.datausher.governance.approval.api.ApprovalCallbackInvocation;
import com.datausher.governance.approval.api.ApprovalCallbackRef;
import com.datausher.governance.approval.api.ApprovalCallbackRegistry;
import com.datausher.governance.approval.api.ApprovalCallbackType;
import com.datausher.governance.approval.api.ApprovalRequest;
import com.datausher.governance.approval.api.ApprovalRequestId;
import com.datausher.governance.approval.api.RetryApprovalCallbackRequest;
import com.datausher.platform.audit.api.AuditCommandService;
import com.datausher.platform.audit.api.AuditOutcome;
import com.datausher.platform.audit.api.AuditRecordRequest;
import com.datausher.platform.audit.api.AuditTarget;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.time.Clock;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DefaultApprovalCallbackRegistry
        implements ApprovalCallbackRegistry, ApprovalCallbackDeliveryService, ApprovalTerminalHandler {
    private static final int MAX_ERROR_LENGTH = 500;

    private final ConcurrentMap<ApprovalCallbackType, ApprovalCallbackHandler> handlers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> deliveryLocks = new ConcurrentHashMap<>();
    private final ApprovalCallbackStore store;
    private final Clock clock;
    private final AuditCommandService audit;

    public DefaultApprovalCallbackRegistry(
            ApprovalCallbackStore store,
            Clock clock,
            AuditCommandService audit
    ) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.audit = Objects.requireNonNull(audit, "audit must not be null");
    }

    @Override
    public void register(ApprovalCallbackHandler handler) {
        Objects.requireNonNull(handler, "handler must not be null");
        ApprovalCallbackType type = Objects.requireNonNull(
                handler.callbackType(), "handler callbackType must not be null");
        ApprovalCallbackHandler existing = handlers.putIfAbsent(type, handler);
        if (existing != null && existing != handler) {
            throw new IllegalStateException("callback handler is already registered: " + type);
        }
    }

    @Override
    public void handle(ApprovalRequest request, RequestContext requestContext) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (request.callback() == null) {
            return;
        }
        ApprovalCallbackRef callback = request.callback();
        ApprovalCallbackInvocation invocation = new ApprovalCallbackInvocation(
                request.requestId(), request.status(), request.targetResource(), callback.type(),
                callback.correlationKey(), callback.parameters(), requestContext);
        deliver(invocation);
    }

    @Override
    public Optional<ApprovalCallbackDelivery> findDelivery(ApprovalRequestId approvalRequestId) {
        Objects.requireNonNull(approvalRequestId, "approvalRequestId must not be null");
        return store.find(approvalRequestId).map(StoredApprovalCallback::delivery);
    }

    @Override
    public ApprovalCallbackDelivery retry(RetryApprovalCallbackRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        StoredApprovalCallback stored = store.find(request.approvalRequestId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "callback delivery does not exist: " + request.approvalRequestId()));
        if (stored.delivery().status() == ApprovalCallbackDeliveryStatus.SUCCEEDED) {
            return stored.delivery();
        }
        ApprovalCallbackInvocation previous = stored.invocation();
        return deliver(new ApprovalCallbackInvocation(
                previous.approvalRequestId(), previous.approvalStatus(), previous.targetResource(),
                previous.callbackType(), previous.correlationKey(), previous.parameters(), request.requestContext()));
    }

    private ApprovalCallbackDelivery deliver(ApprovalCallbackInvocation invocation) {
        String requestId = invocation.approvalRequestId().value();
        Object lock = deliveryLocks.computeIfAbsent(requestId, ignored -> new Object());
        try {
            synchronized (lock) {
                return deliverLocked(invocation);
            }
        } finally {
            deliveryLocks.remove(requestId, lock);
        }
    }

    private ApprovalCallbackDelivery deliverLocked(ApprovalCallbackInvocation invocation) {
        Optional<StoredApprovalCallback> previous = store.find(invocation.approvalRequestId());
        if (previous.map(StoredApprovalCallback::delivery)
                .map(ApprovalCallbackDelivery::status)
                .filter(ApprovalCallbackDeliveryStatus.SUCCEEDED::equals)
                .isPresent()) {
            return previous.orElseThrow().delivery();
        }
        Instant attemptedAt = clock.now();
        int attempts = previous.map(value -> value.delivery().attempts() + 1).orElse(1);
        ApprovalCallbackDelivery delivery;
        try {
            ApprovalCallbackHandler handler = handlers.get(invocation.callbackType());
            if (handler == null) {
                throw new IllegalStateException(
                        "no callback handler is registered for: " + invocation.callbackType());
            }
            handler.handle(invocation);
            delivery = new ApprovalCallbackDelivery(
                    invocation.approvalRequestId(), invocation.callbackType(),
                    ApprovalCallbackDeliveryStatus.SUCCEEDED, attempts, attemptedAt, clock.now(), "");
        } catch (RuntimeException exception) {
            delivery = new ApprovalCallbackDelivery(
                    invocation.approvalRequestId(), invocation.callbackType(),
                    ApprovalCallbackDeliveryStatus.FAILED, attempts, attemptedAt, null, safeError(exception));
        }
        store.replace(previous, new StoredApprovalCallback(invocation, delivery));
        recordAudit(invocation, delivery);
        return delivery;
    }

    private void recordAudit(ApprovalCallbackInvocation invocation, ApprovalCallbackDelivery delivery) {
        Map<String, String> details = delivery.status() == ApprovalCallbackDeliveryStatus.SUCCEEDED
                ? Map.of("callbackType", invocation.callbackType().value(),
                "attempts", Integer.toString(delivery.attempts()))
                : Map.of("callbackType", invocation.callbackType().value(),
                "attempts", Integer.toString(delivery.attempts()), "error", delivery.lastError());
        audit.record(new AuditRecordRequest(
                invocation.requestContext(), "approval", "approval-callback.dispatch",
                AuditTarget.global("approval-request", invocation.approvalRequestId().value()),
                delivery.status() == ApprovalCallbackDeliveryStatus.SUCCEEDED
                        ? AuditOutcome.SUCCEEDED : AuditOutcome.FAILED,
                details));
    }

    private static String safeError(RuntimeException exception) {
        String message = exception.getMessage();
        String value = exception.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message.trim());
        return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }
}
