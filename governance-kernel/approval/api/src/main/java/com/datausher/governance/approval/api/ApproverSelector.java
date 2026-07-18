package com.datausher.governance.approval.api;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.ownership.api.OwnershipRole;

import java.util.Map;
import java.util.Objects;

public record ApproverSelector(ApproverSelectorType type, Map<String, String> criteria) {
    public ApproverSelector {
        type = Objects.requireNonNull(type, "type must not be null");
        criteria = criteria == null ? Map.of() : Map.copyOf(criteria);
    }

    public static ApproverSelector subject(SubjectRef subjectRef) {
        Objects.requireNonNull(subjectRef, "subjectRef must not be null");
        return new ApproverSelector(ApproverSelectorType.SUBJECT, Map.of(
                "subjectType", subjectRef.type().value(),
                "subjectId", subjectRef.subjectId()
        ));
    }

    public static ApproverSelector resourceOwner(OwnershipRole role) {
        Objects.requireNonNull(role, "role must not be null");
        return new ApproverSelector(
                ApproverSelectorType.RESOURCE_OWNER,
                Map.of("role", role.value())
        );
    }
}
