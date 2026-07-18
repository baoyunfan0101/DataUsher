package com.datausher.governance.approval.core;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.approval.api.ApprovalDecisionAuthorizer;
import com.datausher.governance.approval.api.ApprovalRequest;
import com.datausher.governance.approval.api.ApprovalStep;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public final class AuthenticatedSubjectDecisionAuthorizer implements ApprovalDecisionAuthorizer {
    @Override
    public void authorize(
            ApprovalRequest approvalRequest,
            ApprovalStep approvalStep,
            SubjectRef approver,
            RequestContext requestContext
    ) {
        Objects.requireNonNull(approvalRequest, "approvalRequest must not be null");
        Objects.requireNonNull(approvalStep, "approvalStep must not be null");
        Objects.requireNonNull(approver, "approver must not be null");
        Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (!requestContext.actor().subjectRefs().contains(approver.canonicalValue())) {
            throw new SecurityException("authenticated actor cannot decide for subject: " + approver.canonicalValue());
        }
    }
}
