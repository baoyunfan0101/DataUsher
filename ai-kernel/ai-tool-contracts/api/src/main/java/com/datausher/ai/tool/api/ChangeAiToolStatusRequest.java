package com.datausher.ai.tool.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record ChangeAiToolStatusRequest(
        AiToolRef ref,
        AiToolStatus status,
        long expectedRevision,
        RequestContext requestContext
) {
    public ChangeAiToolStatusRequest {
        ref = Objects.requireNonNull(ref, "ref must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
        if (expectedRevision < 1) {
            throw new IllegalArgumentException("expectedRevision must be greater than zero");
        }
    }
}
