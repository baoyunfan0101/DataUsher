package com.datausher.governance.approval.api;

import java.time.Instant;
import java.util.Objects;

public record ApprovalCallbackDelivery(
        ApprovalRequestId approvalRequestId,
        ApprovalCallbackType callbackType,
        ApprovalCallbackDeliveryStatus status,
        int attempts,
        Instant lastAttemptedAt,
        Instant succeededAt,
        String lastError
) {
    public ApprovalCallbackDelivery {
        approvalRequestId = Objects.requireNonNull(approvalRequestId, "approvalRequestId must not be null");
        callbackType = Objects.requireNonNull(callbackType, "callbackType must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        if (attempts < 1) {
            throw new IllegalArgumentException("attempts must be greater than or equal to 1");
        }
        lastAttemptedAt = Objects.requireNonNull(lastAttemptedAt, "lastAttemptedAt must not be null");
        lastError = lastError == null ? "" : lastError.trim();
        if ((status == ApprovalCallbackDeliveryStatus.SUCCEEDED) != (succeededAt != null)) {
            throw new IllegalArgumentException("succeededAt must be present only for successful delivery");
        }
        if (status == ApprovalCallbackDeliveryStatus.SUCCEEDED && !lastError.isEmpty()) {
            throw new IllegalArgumentException("successful delivery must not have a lastError");
        }
    }
}
