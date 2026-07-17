package com.datausher.governance.access.api;

import java.util.Map;
import java.util.Objects;

public record Subject(
        SubjectRef ref,
        String displayName,
        SubjectStatus status,
        Map<String, String> attributes
) {
    public Subject {
        ref = Objects.requireNonNull(ref, "ref must not be null");
        displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        status = Objects.requireNonNull(status, "status must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (displayName.isEmpty()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
    }
}
