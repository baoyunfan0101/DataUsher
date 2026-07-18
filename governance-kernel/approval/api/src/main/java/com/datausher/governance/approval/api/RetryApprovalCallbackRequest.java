package com.datausher.governance.approval.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record RetryApprovalCallbackRequest(
        ApprovalRequestId approvalRequestId,
        RequestContext requestContext
) {
    public RetryApprovalCallbackRequest {
        approvalRequestId = Objects.requireNonNull(approvalRequestId, "approvalRequestId must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
    }
}
