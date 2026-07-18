package com.datausher.governance.approval.core;

import com.datausher.governance.approval.api.ApprovalCallbackDelivery;
import com.datausher.governance.approval.api.ApprovalCallbackInvocation;

import java.util.Objects;

public record StoredApprovalCallback(
        ApprovalCallbackInvocation invocation,
        ApprovalCallbackDelivery delivery
) {
    public StoredApprovalCallback {
        invocation = Objects.requireNonNull(invocation, "invocation must not be null");
        delivery = Objects.requireNonNull(delivery, "delivery must not be null");
        if (!invocation.approvalRequestId().equals(delivery.approvalRequestId())) {
            throw new IllegalArgumentException("approval request identifiers must match");
        }
    }
}
