package com.datausher.ai.runtime.api;

import com.datausher.ai.tool.api.AiToolResult;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record CompleteAiToolInvocationRequest(
        AiToolInvocationId invocationId,
        long expectedRevision,
        AiToolResult result,
        RequestContext requestContext
) {
    public CompleteAiToolInvocationRequest {
        invocationId = Objects.requireNonNull(
                invocationId, "invocationId must not be null");
        result = Objects.requireNonNull(result, "result must not be null");
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
        if (expectedRevision < 1) {
            throw new IllegalArgumentException("expectedRevision must be greater than zero");
        }
    }
}
