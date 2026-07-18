package com.datausher.execution.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record ChangeExecutionAccountStatusRequest(
        ExecutionAccountId accountId,
        ExecutionAccountStatus status,
        long expectedRevision,
        RequestContext requestContext
) {
    public ChangeExecutionAccountStatusRequest {
        accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (expectedRevision < 1) {
            throw new IllegalArgumentException("expectedRevision must be greater than zero");
        }
    }
}
