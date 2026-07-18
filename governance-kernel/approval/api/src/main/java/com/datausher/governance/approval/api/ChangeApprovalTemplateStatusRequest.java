package com.datausher.governance.approval.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record ChangeApprovalTemplateStatusRequest(
        ApprovalTemplateKey templateKey,
        long version,
        ApprovalTemplateStatus status,
        RequestContext requestContext
) {
    public ChangeApprovalTemplateStatusRequest {
        templateKey = Objects.requireNonNull(templateKey, "templateKey must not be null");
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than or equal to 1");
        }
        status = Objects.requireNonNull(status, "status must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
    }
}
