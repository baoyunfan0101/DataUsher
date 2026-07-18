package com.datausher.governance.approval.api;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record DecideApprovalRequest(
        ApprovalRequestId requestId,
        String stepKey,
        SubjectRef approver,
        ApprovalDecisionType decision,
        String comment,
        RequestContext requestContext
) {
    public DecideApprovalRequest {
        requestId = Objects.requireNonNull(requestId, "requestId must not be null");
        stepKey = Objects.requireNonNull(stepKey, "stepKey must not be null").trim().toLowerCase();
        approver = Objects.requireNonNull(approver, "approver must not be null");
        decision = Objects.requireNonNull(decision, "decision must not be null");
        comment = comment == null ? "" : comment.trim();
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (!stepKey.matches("[a-z][a-z0-9.-]{0,126}")) {
            throw new IllegalArgumentException("stepKey must match [a-z][a-z0-9.-]{0,126}");
        }
    }
}
