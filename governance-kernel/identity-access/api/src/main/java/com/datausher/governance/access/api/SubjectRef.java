package com.datausher.governance.access.api;

import java.util.Objects;

public record SubjectRef(SubjectType type, String subjectId) {
    public SubjectRef {
        type = Objects.requireNonNull(type, "type must not be null");
        subjectId = Objects.requireNonNull(subjectId, "subjectId must not be null").trim();
        if (subjectId.isEmpty()) {
            throw new IllegalArgumentException("subjectId must not be blank");
        }
    }

    public String canonicalValue() {
        return type.name().toLowerCase() + ":" + subjectId;
    }
}
