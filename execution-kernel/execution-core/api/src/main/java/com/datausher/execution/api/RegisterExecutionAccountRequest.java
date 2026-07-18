package com.datausher.execution.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record RegisterExecutionAccountRequest(
        ExecutionAccountId accountId,
        String displayName,
        String adapterId,
        String credentialBindingId,
        Set<ExecutionWorkloadType> workloadTypes,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public RegisterExecutionAccountRequest {
        accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        workloadTypes = workloadTypes == null ? Set.of() : Set.copyOf(workloadTypes);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        new ExecutionAccount(accountId, displayName, adapterId, credentialBindingId,
                workloadTypes, ExecutionAccountStatus.ACTIVE, attributes,
                requestContext.requestTime(), requestContext.requestTime(), 1);
    }
}
