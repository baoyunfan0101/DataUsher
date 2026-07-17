package com.datausher.governance.project.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record ChangeProjectStatusRequest(
        String projectId,
        ProjectStatus status,
        RequestContext requestContext
) {
    public ChangeProjectStatusRequest {
        projectId = Objects.requireNonNull(projectId, "projectId must not be null").trim();
        status = Objects.requireNonNull(status, "status must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (projectId.isEmpty()) {
            throw new IllegalArgumentException("projectId must not be blank");
        }
    }
}
