package com.datausher.governance.ownership.api;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.resource.api.ResourceRef;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record ResourceOwner(
        ResourceRef resourceRef,
        SubjectRef subjectRef,
        OwnershipRole role,
        Instant assignedAt,
        String assignedBy,
        Map<String, String> attributes
) {
    public ResourceOwner {
        resourceRef = Objects.requireNonNull(resourceRef, "resourceRef must not be null");
        subjectRef = Objects.requireNonNull(subjectRef, "subjectRef must not be null");
        role = Objects.requireNonNull(role, "role must not be null");
        assignedAt = Objects.requireNonNull(assignedAt, "assignedAt must not be null");
        assignedBy = Objects.requireNonNull(assignedBy, "assignedBy must not be null").trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (assignedBy.isEmpty()) {
            throw new IllegalArgumentException("assignedBy must not be blank");
        }
    }
}
