package com.datausher.governance.approval.api;

import java.util.Optional;

public interface ApprovalCallbackDeliveryService {
    Optional<ApprovalCallbackDelivery> findDelivery(ApprovalRequestId approvalRequestId);

    ApprovalCallbackDelivery retry(RetryApprovalCallbackRequest request);
}
