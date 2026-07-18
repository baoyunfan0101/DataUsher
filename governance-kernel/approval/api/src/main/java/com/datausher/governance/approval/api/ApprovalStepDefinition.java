package com.datausher.governance.approval.api;

import java.util.List;
import java.util.Objects;

public record ApprovalStepDefinition(
        String stepKey,
        String displayName,
        List<ApproverSelector> approverSelectors,
        int requiredApprovals
) {
    public ApprovalStepDefinition {
        stepKey = normalizeKey(stepKey);
        displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        approverSelectors = List.copyOf(Objects.requireNonNull(
                approverSelectors, "approverSelectors must not be null"));
        if (displayName.isEmpty()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (approverSelectors.isEmpty()) {
            throw new IllegalArgumentException("approverSelectors must not be empty");
        }
        if (requiredApprovals < 1) {
            throw new IllegalArgumentException("requiredApprovals must be greater than or equal to 1");
        }
    }

    private static String normalizeKey(String value) {
        String normalized = Objects.requireNonNull(value, "stepKey must not be null").trim().toLowerCase();
        if (!normalized.matches("[a-z][a-z0-9.-]{0,126}")) {
            throw new IllegalArgumentException("stepKey must match [a-z][a-z0-9.-]{0,126}");
        }
        return normalized;
    }
}
