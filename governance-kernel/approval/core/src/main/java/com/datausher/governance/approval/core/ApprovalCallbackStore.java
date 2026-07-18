package com.datausher.governance.approval.core;

import com.datausher.governance.approval.api.ApprovalRequestId;

import java.util.Optional;

public interface ApprovalCallbackStore {
    Optional<StoredApprovalCallback> find(ApprovalRequestId approvalRequestId);

    void replace(Optional<StoredApprovalCallback> expected, StoredApprovalCallback replacement);
}
