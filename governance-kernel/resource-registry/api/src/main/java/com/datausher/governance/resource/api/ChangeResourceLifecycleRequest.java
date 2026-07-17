package com.datausher.governance.resource.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record ChangeResourceLifecycleRequest(
        ResourceRef ref,
        ResourceLifecycle lifecycle,
        RequestContext requestContext
) {
    public ChangeResourceLifecycleRequest {
        ref = Objects.requireNonNull(ref, "ref must not be null");
        lifecycle = Objects.requireNonNull(lifecycle, "lifecycle must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
    }
}
