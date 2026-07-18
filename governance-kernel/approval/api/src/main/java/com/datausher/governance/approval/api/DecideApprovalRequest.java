package com.datausher.governance.approval.api;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record DecideApprovalRequest(
        ApprovalRequestId requestId,
        SubjectRef approver,
        ApprovalDecisionType decision,
        String comment,
        RequestContext requestContext
) {
    public DecideApprovalRequest {
        requestId = Objects.requireNonNull(requestId, "requestId must not be null");
        approver = Objects.requireNonNull(approver, "approver must not be null");
        decision = Objects.requireNonNull(decision, "decision must not be null");
        comment = comment == null ? "" : comment.trim();
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
    }
}
