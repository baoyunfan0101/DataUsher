package com.datausher.ai.tool.api;

import java.util.Objects;

public record AiToolDefinition(
        AiToolSchema schema,
        AiToolPermissionPolicy permissionPolicy
) {
    public AiToolDefinition {
        schema = Objects.requireNonNull(schema, "schema must not be null");
        permissionPolicy = permissionPolicy == null
                ? AiToolPermissionPolicy.none() : permissionPolicy;
    }
}
