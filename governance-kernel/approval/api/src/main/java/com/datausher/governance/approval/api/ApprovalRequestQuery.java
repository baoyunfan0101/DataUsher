package com.datausher.governance.approval.api;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.resource.api.ResourceRef;

public record ApprovalRequestQuery(
        ApprovalPurpose purpose,
        ApprovalRequestStatus status,
        ResourceRef targetResource,
        SubjectRef requestedBy,
        SubjectRef eligibleApprover
) {
    public static ApprovalRequestQuery all() {
        return new ApprovalRequestQuery(null, null, null, null, null);
    }
}
