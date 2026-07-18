package com.datausher.governance.approval.api;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.platform.shared.context.RequestContext;

public interface ApprovalDecisionAuthorizer {
    void authorize(
            ApprovalRequest approvalRequest,
            ApprovalStep approvalStep,
            SubjectRef approver,
            RequestContext requestContext
    );
}
