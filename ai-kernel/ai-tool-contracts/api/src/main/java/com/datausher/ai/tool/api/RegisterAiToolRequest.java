package com.datausher.ai.tool.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record RegisterAiToolRequest(
        AiToolSchema schema,
        AiToolPermissionPolicy permissionPolicy,
        RequestContext requestContext
) {
    public RegisterAiToolRequest {
        schema = Objects.requireNonNull(schema, "schema must not be null");
        permissionPolicy = permissionPolicy == null
                ? AiToolPermissionPolicy.none() : permissionPolicy;
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
    }
}
