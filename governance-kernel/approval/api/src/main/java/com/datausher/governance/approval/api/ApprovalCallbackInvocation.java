package com.datausher.governance.approval.api;

import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;

public record ApprovalCallbackInvocation(
        ApprovalRequestId approvalRequestId,
        ApprovalRequestStatus approvalStatus,
        ResourceRef targetResource,
        ApprovalCallbackType callbackType,
        String correlationKey,
        Map<String, String> parameters,
        RequestContext requestContext
) {
    public ApprovalCallbackInvocation {
        approvalRequestId = Objects.requireNonNull(approvalRequestId, "approvalRequestId must not be null");
        approvalStatus = Objects.requireNonNull(approvalStatus, "approvalStatus must not be null");
        if (approvalStatus == ApprovalRequestStatus.PENDING) {
            throw new IllegalArgumentException("approvalStatus must be terminal");
        }
        targetResource = Objects.requireNonNull(targetResource, "targetResource must not be null");
        callbackType = Objects.requireNonNull(callbackType, "callbackType must not be null");
        correlationKey = Objects.requireNonNull(correlationKey, "correlationKey must not be null").trim();
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (correlationKey.isEmpty()) {
            throw new IllegalArgumentException("correlationKey must not be blank");
        }
    }
}
