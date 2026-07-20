package com.datausher.data.quality.core;

import com.datausher.data.quality.api.ProfileJob;
import com.datausher.execution.api.ExecutionRequest;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record ProfileResultDecodingRequest(
        ProfileJob job,
        ExecutionRequest executionRequest,
        RequestContext requestContext
) {
    public ProfileResultDecodingRequest {
        job = Objects.requireNonNull(job, "job must not be null");
        executionRequest = Objects.requireNonNull(
                executionRequest, "executionRequest must not be null");
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
    }
}
