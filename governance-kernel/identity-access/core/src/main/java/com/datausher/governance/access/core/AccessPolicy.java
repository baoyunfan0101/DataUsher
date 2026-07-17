package com.datausher.governance.access.core;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.governance.resource.api.ResourceScope;

import java.util.Objects;

public record AccessPolicy(
        String policyId,
        SubjectRef subject,
        String resourceType,
        String action,
        ResourceScope scope,
        PolicyEffect effect,
        int priority,
        boolean active
) {
    public AccessPolicy {
        policyId = normalize(policyId, "policyId");
        subject = Objects.requireNonNull(subject, "subject must not be null");
        resourceType = new ResourceRef(resourceType, "validation", ResourceScope.global()).resourceType();
        action = normalize(action, "action").toLowerCase();
        scope = Objects.requireNonNull(scope, "scope must not be null");
        effect = Objects.requireNonNull(effect, "effect must not be null");
    }

    public boolean matches(SubjectRef candidateSubject, String candidateAction, ResourceRef resource) {
        return active
                && subject.equals(candidateSubject)
                && resourceType.equals(resource.resourceType())
                && action.equals(candidateAction)
                && scope.equals(resource.scope());
    }

    private static String normalize(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
