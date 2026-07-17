package com.datausher.governance.resource.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record RegisterResourceTypeRequest(
        ResourceTypeDefinition definition,
        RequestContext requestContext
) {
    public RegisterResourceTypeRequest {
        definition = Objects.requireNonNull(definition, "definition must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
    }
}
