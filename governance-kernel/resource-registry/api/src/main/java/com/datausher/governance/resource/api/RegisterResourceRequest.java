package com.datausher.governance.resource.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;

public record RegisterResourceRequest(
        ResourceRef ref,
        String displayName,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public RegisterResourceRequest {
        ref = Objects.requireNonNull(ref, "ref must not be null");
        displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (displayName.isEmpty()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
    }
}
