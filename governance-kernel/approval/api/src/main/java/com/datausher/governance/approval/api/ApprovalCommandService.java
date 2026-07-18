package com.datausher.governance.approval.api;

public interface ApprovalCommandService {
    ApprovalTemplate publishTemplate(PublishApprovalTemplateRequest request);

    ApprovalRequest submit(SubmitApprovalRequest request);

    ApprovalRequest decide(DecideApprovalRequest request);

    ApprovalRequest cancel(CancelApprovalRequest request);
}
