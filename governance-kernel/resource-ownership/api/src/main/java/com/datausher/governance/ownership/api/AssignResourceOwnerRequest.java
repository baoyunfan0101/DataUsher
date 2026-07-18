package com.datausher.governance.ownership.api;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;

public record AssignResourceOwnerRequest(
        ResourceRef resourceRef,
        SubjectRef subjectRef,
        OwnershipRole role,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public AssignResourceOwnerRequest {
        resourceRef = Objects.requireNonNull(resourceRef, "resourceRef must not be null");
        subjectRef = Objects.requireNonNull(subjectRef, "subjectRef must not be null");
        role = Objects.requireNonNull(role, "role must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
    }
}
