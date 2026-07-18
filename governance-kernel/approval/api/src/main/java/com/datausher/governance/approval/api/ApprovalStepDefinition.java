package com.datausher.governance.approval.api;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ApprovalStepDefinition(
        String stepKey,
        String displayName,
        List<ApproverSelector> approverSelectors,
        int requiredApprovals,
        Set<String> dependsOn
) {
    public ApprovalStepDefinition {
        stepKey = normalizeKey(stepKey);
        displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        approverSelectors = List.copyOf(Objects.requireNonNull(
                approverSelectors, "approverSelectors must not be null"));
        dependsOn = Objects.requireNonNull(dependsOn, "dependsOn must not be null").stream()
                .map(ApprovalStepDefinition::normalizeKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (displayName.isEmpty()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (approverSelectors.isEmpty()) {
            throw new IllegalArgumentException("approverSelectors must not be empty");
        }
        if (requiredApprovals < 1) {
            throw new IllegalArgumentException("requiredApprovals must be greater than or equal to 1");
        }
        if (dependsOn.contains(stepKey)) {
            throw new IllegalArgumentException("approval step must not depend on itself");
        }
    }

    public ApprovalStepDefinition(
            String stepKey,
            String displayName,
            List<ApproverSelector> approverSelectors,
            int requiredApprovals
    ) {
        this(stepKey, displayName, approverSelectors, requiredApprovals, Set.of());
    }

    private static String normalizeKey(String value) {
        String normalized = Objects.requireNonNull(value, "stepKey must not be null").trim().toLowerCase();
        if (!normalized.matches("[a-z][a-z0-9.-]{0,126}")) {
            throw new IllegalArgumentException("stepKey must match [a-z][a-z0-9.-]{0,126}");
        }
        return normalized;
    }
}
