package com.datausher.governance.approval.api;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;

public record ApprovalRequestedEvent(
        String eventId,
        Instant occurredAt,
        RequestContext requestContext,
        ApprovalRequest approvalRequest
) implements DomainEvent {
    public ApprovalRequestedEvent {
        eventId = normalize(eventId, "eventId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        approvalRequest = Objects.requireNonNull(approvalRequest, "approvalRequest must not be null");
        if (approvalRequest.status() != ApprovalRequestStatus.PENDING) {
            throw new IllegalArgumentException("approvalRequest must be pending");
        }
    }

    @Override
    public String eventType() {
        return ApprovalEvents.REQUESTED;
    }

    @Override
    public String sourceModule() {
        return "approval";
    }

    private static String normalize(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
