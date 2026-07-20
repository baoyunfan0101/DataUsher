package com.datausher.data.quality.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record CancelProfileJobRequest(
        ProfileJobId jobId,
        long expectedRevision,
        RequestContext requestContext
) {
    public CancelProfileJobRequest {
        jobId = Objects.requireNonNull(jobId, "jobId must not be null");
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
        if (expectedRevision < 1) {
            throw new IllegalArgumentException("expectedRevision must be greater than zero");
        }
    }
}
