package com.datausher.governance.access.api;

import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.platform.shared.context.RequestContext;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record AccessRequest(
        Set<SubjectRef> subjects,
        String action,
        ResourceRef resource,
        RequestContext requestContext,
        Map<String, String> attributes
) {
    public AccessRequest {
        subjects = Set.copyOf(Objects.requireNonNull(subjects, "subjects must not be null"));
        action = normalizeAction(action);
        resource = Objects.requireNonNull(resource, "resource must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (subjects.isEmpty()) {
            throw new IllegalArgumentException("subjects must not be empty");
        }
        if (new HashSet<>(subjects).size() != subjects.size()) {
            throw new IllegalArgumentException("subjects must not contain duplicates");
        }
    }

    private static String normalizeAction(String value) {
        String normalized = Objects.requireNonNull(value, "action must not be null").trim().toLowerCase();
        if (!normalized.matches("[a-z][a-z0-9.-]{0,126}")) {
            throw new IllegalArgumentException("action must match [a-z][a-z0-9.-]{0,126}");
        }
        return normalized;
    }
}
