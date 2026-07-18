package com.datausher.governance.ownership.api;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.resource.api.ResourceRef;

public record OwnershipQuery(
        ResourceRef resourceRef,
        SubjectRef subjectRef,
        OwnershipRole role
) {
    public static OwnershipQuery forResource(ResourceRef resourceRef) {
        return new OwnershipQuery(resourceRef, null, null);
    }

    public static OwnershipQuery forSubject(SubjectRef subjectRef) {
        return new OwnershipQuery(null, subjectRef, null);
    }
}
