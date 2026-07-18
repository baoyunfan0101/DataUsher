package com.datausher.governance.approval.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record CancelApprovalRequest(
        ApprovalRequestId requestId,
        String reason,
        RequestContext requestContext
) {
    public CancelApprovalRequest {
        requestId = Objects.requireNonNull(requestId, "requestId must not be null");
        reason = Objects.requireNonNull(reason, "reason must not be null").trim();
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (reason.isEmpty()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
    }
}
